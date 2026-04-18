package com.mortal.warning.service.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mortal.warning.common.enums.WarningActionType;
import com.mortal.warning.common.enums.WarningStatus;
import com.mortal.warning.entity.WarningProcessLog;
import com.mortal.warning.entity.WarningRecord;
import com.mortal.warning.mapper.WarningProcessLogMapper;
import com.mortal.warning.mapper.WarningRecordMapper;
import com.mortal.warning.config.WarningSchedulerLockProperties;
import com.mortal.warning.support.WarningSchedulerLockSupport;
import com.mortal.warning.support.WarningStatsCacheSupport;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 预警自动归档调度器。
 */
@Component
public class WarningArchiveScheduler {

    private static final Logger log = LoggerFactory.getLogger(WarningArchiveScheduler.class);
    private static final String SCHEDULER_LOCK_NAME = "warning:archive";

    private final WarningRecordMapper warningRecordMapper;
    private final WarningProcessLogMapper warningProcessLogMapper;
    private final WarningSchedulerLockSupport warningSchedulerLockSupport;
    private final WarningSchedulerLockProperties warningSchedulerLockProperties;
    private final WarningStatsCacheSupport warningStatsCacheSupport;

    @Value("${warning.archive.enabled:true}")
    private boolean archiveEnabled;

    @Value("${warning.archive.resolved-days:7}")
    private int resolvedDays;

    @Value("${warning.archive.batch-size:100}")
    private int batchSize;

    public WarningArchiveScheduler(WarningRecordMapper warningRecordMapper,
                                   WarningProcessLogMapper warningProcessLogMapper,
                                   WarningSchedulerLockSupport warningSchedulerLockSupport,
                                   WarningSchedulerLockProperties warningSchedulerLockProperties,
                                   WarningStatsCacheSupport warningStatsCacheSupport) {
        this.warningRecordMapper = warningRecordMapper;
        this.warningProcessLogMapper = warningProcessLogMapper;
        this.warningSchedulerLockSupport = warningSchedulerLockSupport;
        this.warningSchedulerLockProperties = warningSchedulerLockProperties;
        this.warningStatsCacheSupport = warningStatsCacheSupport;
    }

    /**
     * 扫描已解决预警并自动归档。
     */
    @Scheduled(
        fixedDelayString = "${warning.archive.scan-ms:600000}",
        initialDelayString = "${warning.archive.initial-delay-ms:120000}"
    )
    public void autoArchiveResolvedWarnings() {
        if (!archiveEnabled || resolvedDays < 0) {
            return;
        }
        try {
            boolean executed = warningSchedulerLockSupport.executeWithLock(
                SCHEDULER_LOCK_NAME,
                warningSchedulerLockProperties.getArchiveLeaseSeconds(),
                () -> {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime cutoffTime = now.minusDays(resolvedDays);
                List<WarningRecord> candidates = warningRecordMapper.selectList(new LambdaQueryWrapper<WarningRecord>()
                    .eq(WarningRecord::getDeleted, 0)
                    .eq(WarningRecord::getStatus, WarningStatus.RESOLVED.name())
                    .isNotNull(WarningRecord::getResolvedTime)
                    .le(WarningRecord::getResolvedTime, cutoffTime)
                    .orderByAsc(WarningRecord::getResolvedTime, WarningRecord::getId)
                    .last("limit " + Math.max(1, batchSize)));
                for (WarningRecord record : candidates) {
                    archiveSingle(record, now, cutoffTime);
                }
                }
            );
            if (!executed) {
                log.debug("Skip warning archive scan because scheduler lock is held by another instance.");
            }
        } catch (Exception ex) {
            log.error("Warning auto archive scan failed.", ex);
        }
    }

    private void archiveSingle(WarningRecord record, LocalDateTime now, LocalDateTime cutoffTime) {
        if (record == null || record.getId() == null) {
            return;
        }
        String closeReason = StringUtils.hasText(record.getCloseReason())
            ? record.getCloseReason().trim()
            : "系统自动归档";
        int updated = warningRecordMapper.update(null, new LambdaUpdateWrapper<WarningRecord>()
            .eq(WarningRecord::getId, record.getId())
            .eq(WarningRecord::getDeleted, 0)
            .eq(WarningRecord::getStatus, WarningStatus.RESOLVED.name())
            .set(WarningRecord::getStatus, WarningStatus.CLOSED.name())
            .set(WarningRecord::getCloseReason, closeReason)
            .set(WarningRecord::getUpdateTime, now));
        if (updated <= 0) {
            return;
        }
        WarningProcessLog processLog = new WarningProcessLog();
        processLog.setWarningId(record.getId());
        processLog.setActionType(WarningActionType.AUTO_ARCHIVE.name());
        processLog.setOperatorId(null);
        processLog.setOperatorName("system");
        processLog.setActionComment(String.format(
            "预警已解决超过 %d 天（阈值时间 %s），系统自动归档",
            resolvedDays,
            cutoffTime
        ));
        processLog.setCreateTime(now);
        processLog.setUpdateTime(now);
        processLog.setDeleted(0);
        warningProcessLogMapper.insert(processLog);
        warningStatsCacheSupport.bumpVersion();
    }
}
