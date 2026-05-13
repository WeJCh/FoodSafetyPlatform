package com.mortal.warning.service.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortal.warning.common.enums.WarningActionType;
import com.mortal.warning.common.enums.WarningLevel;
import com.mortal.warning.common.enums.WarningStatus;
import com.mortal.warning.entity.WarningProcessLog;
import com.mortal.warning.entity.WarningRecord;
import com.mortal.warning.mapper.WarningProcessLogMapper;
import com.mortal.warning.mapper.WarningRecordMapper;
import com.mortal.warning.config.WarningSchedulerLockProperties;
import com.mortal.warning.support.WarningSchedulerLockSupport;
import com.mortal.warning.support.WarningStatsCacheSupport;
import java.time.Duration;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 预警升级调度器。
 *
 * <p>职责：扫描整改类预警，满足阈值后自动从 L1 升级为 L2，并写入 AUTO_LEVEL_UP 日志。</p>
 */
@Component
public class WarningEscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(WarningEscalationScheduler.class);
    private static final String SCHEDULER_LOCK_NAME = "warning:escalation";

    private static final String BIZ_TYPE_RECTIFICATION = "RECTIFICATION";
    private static final Set<WarningStatus> ACTIVE_STATUSES = EnumSet.of(
        WarningStatus.OPEN,
        WarningStatus.PROCESSING
    );
    private static final List<DateTimeFormatter> DEADLINE_FORMATTERS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    );

    private final WarningRecordMapper warningRecordMapper;
    private final WarningProcessLogMapper warningProcessLogMapper;
    private final ObjectMapper objectMapper;
    private final WarningSchedulerLockSupport warningSchedulerLockSupport;
    private final WarningSchedulerLockProperties warningSchedulerLockProperties;
    private final WarningStatsCacheSupport warningStatsCacheSupport;

    @Value("${warning.escalation.enabled:true}")
    private boolean escalationEnabled;

    @Value("${warning.escalation.rectification-l2-minutes:4320}")
    private long rectificationL2Minutes;

    public WarningEscalationScheduler(WarningRecordMapper warningRecordMapper,
                                      WarningProcessLogMapper warningProcessLogMapper,
                                      ObjectMapper objectMapper,
                                      WarningSchedulerLockSupport warningSchedulerLockSupport,
                                      WarningSchedulerLockProperties warningSchedulerLockProperties,
                                      WarningStatsCacheSupport warningStatsCacheSupport) {
        this.warningRecordMapper = warningRecordMapper;
        this.warningProcessLogMapper = warningProcessLogMapper;
        this.objectMapper = objectMapper;
        this.warningSchedulerLockSupport = warningSchedulerLockSupport;
        this.warningSchedulerLockProperties = warningSchedulerLockProperties;
        this.warningStatsCacheSupport = warningStatsCacheSupport;
    }

    /**
     * 定时扫描整改预警升级条件。
     */
    @Scheduled(
        fixedDelayString = "${warning.escalation.scan-ms:600000}",
        initialDelayString = "${warning.escalation.initial-delay-ms:60000}"
    )
    public void escalateRectificationWarnings() {
        if (!escalationEnabled || rectificationL2Minutes <= 0) {
            return;
        }
        try {
            boolean executed = warningSchedulerLockSupport.executeWithLock(
                SCHEDULER_LOCK_NAME,
                warningSchedulerLockProperties.getEscalationLeaseSeconds(),
                () -> {
                LocalDateTime now = LocalDateTime.now();
                List<WarningRecord> candidates = loadCandidates();
                for (WarningRecord record : candidates) {
                    tryEscalateSingle(record, now);
                }
                }
            );
            if (!executed) {
                log.debug("Skip warning escalation scan because scheduler lock is held by another instance.");
            }
        } catch (Exception ex) {
            log.error("Warning escalation scan failed.", ex);
        }
    }
    /**
     * 加载整改预警。
     */
    private List<WarningRecord> loadCandidates() {
        return warningRecordMapper.selectList(new LambdaQueryWrapper<WarningRecord>()
            .eq(WarningRecord::getDeleted, 0)
            .eq(WarningRecord::getBizType, BIZ_TYPE_RECTIFICATION)
            .eq(WarningRecord::getLevel, WarningLevel.L1.name())
            .in(WarningRecord::getStatus, ACTIVE_STATUSES));
    }
    /**
     * 尝试升级整改预警。
     */
    private void tryEscalateSingle(WarningRecord record, LocalDateTime now) {
        Long warningId = record == null ? null : record.getId();
        if (warningId == null) {
            return;
        }
        LocalDateTime deadline = resolveDeadline(record == null ? null : record.getPayloadJson());
        if (deadline == null) {
            log.warn(
                "Skip warning auto-level-up because payload.deadline missing or invalid. warningId={}, dedupKey={}",
                warningId,
                record.getDedupKey()
            );
            return;
        }
        long overdueMinutes = Math.max(0L, Duration.between(deadline, now).toMinutes());
        if (overdueMinutes < rectificationL2Minutes) {
            return;
        }

        int updated = warningRecordMapper.update(null, new LambdaUpdateWrapper<WarningRecord>()
            .eq(WarningRecord::getId, warningId)
            .eq(WarningRecord::getDeleted, 0)
            .eq(WarningRecord::getLevel, WarningLevel.L1.name())
            .set(WarningRecord::getLevel, WarningLevel.L2.name())
            .set(WarningRecord::getUpdateTime, now));
        if (updated <= 0) {
            return;
        }

        WarningProcessLog processLog = new WarningProcessLog();
        processLog.setWarningId(warningId);
        processLog.setActionType(WarningActionType.AUTO_LEVEL_UP.name());
        processLog.setOperatorId(null);
        processLog.setOperatorName("系统");
        processLog.setActionComment(String.format(
            "整改超时达到二级阈值（%d 分钟），当前超时 %d 分钟，截止时间 %s，系统自动升级为 L2",
            rectificationL2Minutes,
            overdueMinutes,
            deadline
        ));
        processLog.setCreateTime(now);
        processLog.setUpdateTime(now);
        processLog.setDeleted(0);
        warningProcessLogMapper.insert(processLog);
        warningStatsCacheSupport.bumpVersion();
    }

    /**
     * 解析预警截止时间。
     */
    private LocalDateTime resolveDeadline(String payloadJson) {
        Map<String, Object> payload = parsePayload(payloadJson);
        if (payload == null) {
            return null;
        }
        return parseLocalDateTime(payload.get("deadline"));
    }

    /**
     * 解析预警负载。
     */
    private Map<String, Object> parsePayload(String payloadJson) {
        if (!StringUtils.hasText(payloadJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return null;
        }
    }
    /**
     * 解析本地日期时间。
     */
    private LocalDateTime parseLocalDateTime(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (raw instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toLocalDateTime();
        }
        if (raw instanceof Number number) {
            long value = number.longValue();
            Instant instant = value > 9999999999L
                ? Instant.ofEpochMilli(value)
                : Instant.ofEpochSecond(value);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        }
        if (raw instanceof List<?> list) {
            List<Integer> ints = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Number number) {
                    ints.add(number.intValue());
                } else if (item instanceof String text && StringUtils.hasText(text)) {
                    try {
                        ints.add(Integer.parseInt(text.trim()));
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                } else {
                    return null;
                }
            }
            if (ints.size() >= 5) {
                int second = ints.size() > 5 ? ints.get(5) : 0;
                try {
                    return LocalDateTime.of(ints.get(0), ints.get(1), ints.get(2), ints.get(3), ints.get(4), second);
                } catch (DateTimeException ex) {
                    return null;
                }
            }
            return null;
        }
        if (raw instanceof String text && StringUtils.hasText(text)) {
            String value = text.trim();
            try {
                return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException ignored) {
                // 中文注释：兼容 payload 中的常见格式，避免因格式差异导致升级失效。
            }
            for (DateTimeFormatter formatter : DEADLINE_FORMATTERS) {
                try {
                    return LocalDateTime.parse(value, formatter);
                } catch (DateTimeParseException ignored) {
                    // try next
                }
            }
        }
        return null;
    }
}
