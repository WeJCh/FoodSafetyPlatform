package com.mortal.regulation.service.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.regulation.client.WarningServiceClient;
import com.mortal.regulation.common.ApiResponse;
import com.mortal.regulation.common.enums.RectificationStatus;
import com.mortal.regulation.dto.WarningEventUpsertDTO;
import com.mortal.regulation.entity.FoodEnterprise;
import com.mortal.regulation.entity.InspectionRecord;
import com.mortal.regulation.entity.RectificationActionLog;
import com.mortal.regulation.entity.RectificationTask;
import com.mortal.regulation.mapper.FoodEnterpriseMapper;
import com.mortal.regulation.mapper.InspectionRecordMapper;
import com.mortal.regulation.mapper.RectificationActionLogMapper;
import com.mortal.regulation.mapper.RectificationTaskMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 整改任务 SLA 定时扫描器。
 *
 * <p>本版本目标：</p>
 * <ul>
 *   <li>继续在 regulation-service 写本地审计日志；</li>
 *   <li>并将超时/升级事件上报到 warning-service，形成跨服务预警主记录。</li>
 * </ul>
 */
@Component
public class RectificationSlaScheduler {

    private static final Logger log = LoggerFactory.getLogger(RectificationSlaScheduler.class);

    private static final String BIZ_TYPE_RECTIFICATION = "RECTIFICATION";
    private static final String SOURCE_SERVICE = "regulation-service";

    private static final String ACTION_SLA_OVERDUE_SUBMIT = "SLA_OVERDUE_SUBMIT";
    private static final String ACTION_SLA_OVERDUE_REVIEW = "SLA_OVERDUE_REVIEW";
    private static final String ACTION_SLA_ESCALATE_SUBMIT_L1 = "SLA_ESCALATE_SUBMIT_L1";
    private static final String ACTION_SLA_ESCALATE_SUBMIT_L2 = "SLA_ESCALATE_SUBMIT_L2";
    private static final String ACTION_SLA_ESCALATE_REVIEW_L1 = "SLA_ESCALATE_REVIEW_L1";
    private static final String ACTION_SLA_ESCALATE_REVIEW_L2 = "SLA_ESCALATE_REVIEW_L2";

    private static final Set<String> SLA_ACTION_TYPES = Set.of(
        ACTION_SLA_OVERDUE_SUBMIT,
        ACTION_SLA_OVERDUE_REVIEW,
        ACTION_SLA_ESCALATE_SUBMIT_L1,
        ACTION_SLA_ESCALATE_SUBMIT_L2,
        ACTION_SLA_ESCALATE_REVIEW_L1,
        ACTION_SLA_ESCALATE_REVIEW_L2
    );

    private final RectificationTaskMapper rectificationTaskMapper;
    private final RectificationActionLogMapper rectificationActionLogMapper;
    private final WarningServiceClient warningServiceClient;
    private final FoodEnterpriseMapper foodEnterpriseMapper;
    private final InspectionRecordMapper inspectionRecordMapper;
    private final Set<String> failedWarningPushKeys = ConcurrentHashMap.newKeySet();
    private final String warningInternalToken;

    /**
     * L1 升级阈值（分钟），默认 24h。
     */
    @Value("${regulation.rectification.sla.escalate-l1-minutes:1440}")
    private long escalateL1Minutes;

    /**
     * L2 升级阈值（分钟），默认 72h。
     */
    @Value("${regulation.rectification.sla.escalate-l2-minutes:4320}")
    private long escalateL2Minutes;

    public RectificationSlaScheduler(RectificationTaskMapper rectificationTaskMapper,
                                     RectificationActionLogMapper rectificationActionLogMapper,
                                     WarningServiceClient warningServiceClient,
                                     FoodEnterpriseMapper foodEnterpriseMapper,
                                     InspectionRecordMapper inspectionRecordMapper,
                                     @Value("${warning.internal.token:warning-internal-token}")
                                     String warningInternalToken) {
        this.rectificationTaskMapper = rectificationTaskMapper;
        this.rectificationActionLogMapper = rectificationActionLogMapper;
        this.warningServiceClient = warningServiceClient;
        this.foodEnterpriseMapper = foodEnterpriseMapper;
        this.inspectionRecordMapper = inspectionRecordMapper;
        this.warningInternalToken = warningInternalToken;
    }

    /**
     * 每 10 分钟扫描一次整改 SLA。
     */
    @Scheduled(fixedDelayString = "${regulation.rectification.sla.scan-ms:600000}", initialDelay = 30000)
    public void scanRectificationSla() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<RectificationTask> activeTasks = loadActiveTasks();
            if (activeTasks.isEmpty()) {
                return;
            }
            Map<Long, Set<String>> loggedActions = loadLoggedSlaActions(activeTasks);
            for (RectificationTask task : activeTasks) {
                processTask(now, task, loggedActions);
            }
        } catch (Exception ex) {
            log.error("Rectification SLA scan failed.", ex);
        }
    }

    private List<RectificationTask> loadActiveTasks() {
        Set<RectificationStatus> activeStatuses = EnumSet.of(
            RectificationStatus.ONGOING,
            RectificationStatus.REWORK,
            RectificationStatus.SUBMITTED
        );
        return rectificationTaskMapper.selectList(new LambdaQueryWrapper<RectificationTask>()
            .eq(RectificationTask::getDeleted, 0)
            .in(RectificationTask::getStatus, activeStatuses));
    }

    private Map<Long, Set<String>> loadLoggedSlaActions(Collection<RectificationTask> tasks) {
        List<Long> taskIds = tasks.stream()
            .map(RectificationTask::getId)
            .filter(Objects::nonNull)
            .toList();
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        List<RectificationActionLog> existingLogs = rectificationActionLogMapper.selectList(
            new LambdaQueryWrapper<RectificationActionLog>()
                .eq(RectificationActionLog::getDeleted, 0)
                .in(RectificationActionLog::getRectificationId, taskIds)
                .in(RectificationActionLog::getActionType, SLA_ACTION_TYPES)
        );
        Map<Long, Set<String>> map = new HashMap<>();
        for (RectificationActionLog logItem : existingLogs) {
            map.computeIfAbsent(logItem.getRectificationId(), key -> new HashSet<>()).add(logItem.getActionType());
        }
        return map;
    }

    private void processTask(LocalDateTime now, RectificationTask task, Map<Long, Set<String>> loggedActions) {
        StageSnapshot snapshot = buildStageSnapshot(now, task);
        if (snapshot == null) {
            return;
        }
        ensureActionLogAndWarning(task, snapshot.overdueActionType(), snapshot.overdueComment(), loggedActions, now);
        if (snapshot.overdueMinutes() >= escalateL1Minutes) {
            ensureActionLogAndWarning(task, snapshot.escalateL1ActionType(), snapshot.escalateL1Comment(), loggedActions, now);
        }
        if (snapshot.overdueMinutes() >= escalateL2Minutes) {
            ensureActionLogAndWarning(task, snapshot.escalateL2ActionType(), snapshot.escalateL2Comment(), loggedActions, now);
        }
    }

    private StageSnapshot buildStageSnapshot(LocalDateTime now, RectificationTask task) {
        RectificationStatus status = task.getStatus();
        if (status == null) {
            return null;
        }

        if (status == RectificationStatus.ONGOING || status == RectificationStatus.REWORK) {
            long overdueMinutes = minutesOverdue(task.getSubmitDeadline(), now);
            if (overdueMinutes <= 0) {
                return null;
            }
            return new StageSnapshot(
                overdueMinutes,
                ACTION_SLA_OVERDUE_SUBMIT,
                ACTION_SLA_ESCALATE_SUBMIT_L1,
                ACTION_SLA_ESCALATE_SUBMIT_L2,
                "企业整改提交已超时",
                "企业整改提交超时，已触发一级升级提醒",
                "企业整改提交严重超时，已触发二级升级提醒"
            );
        }

        if (status == RectificationStatus.SUBMITTED) {
            long overdueMinutes = minutesOverdue(task.getReviewDeadline(), now);
            if (overdueMinutes <= 0) {
                return null;
            }
            return new StageSnapshot(
                overdueMinutes,
                ACTION_SLA_OVERDUE_REVIEW,
                ACTION_SLA_ESCALATE_REVIEW_L1,
                ACTION_SLA_ESCALATE_REVIEW_L2,
                "监管复核已超时",
                "监管复核超时，已触发一级升级提醒",
                "监管复核严重超时，已触发二级升级提醒"
            );
        }

        return null;
    }

    private long minutesOverdue(LocalDateTime deadline, LocalDateTime now) {
        if (deadline == null || now == null) {
            return 0L;
        }
        return Duration.between(deadline, now).toMinutes();
    }

    /**
     * 幂等写本地动作日志，并上报 warning-service。
     */
    private void ensureActionLogAndWarning(RectificationTask task,
                                           String actionType,
                                           String comment,
                                           Map<Long, Set<String>> loggedActions,
                                           LocalDateTime now) {
        Long taskId = task == null ? null : task.getId();
        if (taskId == null || actionType == null) {
            return;
        }
        String dedupKey = buildDedupKey(taskId, actionType);
        Set<String> actionTypes = loggedActions.computeIfAbsent(taskId, key -> new HashSet<>());
        if (actionTypes.contains(actionType) && !failedWarningPushKeys.contains(dedupKey)) {
            return;
        }

        if (!actionTypes.contains(actionType)) {
            RectificationActionLog logItem = new RectificationActionLog();
            logItem.setRectificationId(taskId);
            logItem.setActionType(actionType);
            logItem.setOperatorId(null);
            logItem.setActionComment(comment);
            logItem.setAttachmentUrls(null);
            logItem.setCreateTime(now);
            logItem.setDeleted(0);
            rectificationActionLogMapper.insert(logItem);
            actionTypes.add(actionType);
        }

        boolean pushed = pushWarningEvent(task, actionType, comment, now);
        if (pushed) {
            failedWarningPushKeys.remove(dedupKey);
        } else {
            failedWarningPushKeys.add(dedupKey);
        }
    }

    private boolean pushWarningEvent(RectificationTask task, String actionType, String comment, LocalDateTime now) {
        try {
            WarningEventUpsertDTO dto = buildWarningEventDto(task, actionType, comment, now);
            ApiResponse<Map<String, Object>> response = warningServiceClient.upsertInternalEvent(dto, warningInternalToken);
            if (response == null || response.getCode() != 0) {
                log.warn("Push warning event failed. taskId={}, actionType={}, response={}",
                    task.getId(), actionType, response);
                return false;
            }
            return true;
        } catch (Exception ex) {
            // 不中断 SLA 主流程，避免联动服务短暂异常影响监管主链路。
            log.warn("Push warning event exception. taskId={}, actionType={}", task.getId(), actionType, ex);
            return false;
        }
    }

    private WarningEventUpsertDTO buildWarningEventDto(RectificationTask task,
                                                       String actionType,
                                                       String comment,
                                                       LocalDateTime now) {
        WarningEventUpsertDTO dto = new WarningEventUpsertDTO();
        dto.setEventType(actionType);
        dto.setBizType(BIZ_TYPE_RECTIFICATION);
        dto.setBizId(task.getId());
        dto.setRegionId(resolveRegionId(task.getEnterpriseId()));
        dto.setOwnerRegulatorId(resolveOwnerRegulatorId(task.getInspectionId()));
        dto.setDedupKey(buildDedupKey(task.getId(), actionType));
        dto.setLevel(resolveLevelByAction(actionType));
        dto.setTitle(resolveTitleByAction(actionType));
        dto.setContent(comment);
        dto.setSourceService(SOURCE_SERVICE);
        dto.setOccurTime(now);
        dto.setPayload(buildPayload(task, actionType, now));
        return dto;
    }

    private String buildDedupKey(Long taskId, String actionType) {
        return BIZ_TYPE_RECTIFICATION + ":" + taskId + ":" + actionType;
    }

    private String resolveLevelByAction(String actionType) {
        if (ACTION_SLA_ESCALATE_SUBMIT_L2.equals(actionType) || ACTION_SLA_ESCALATE_REVIEW_L2.equals(actionType)) {
            return "L2";
        }
        return "L1";
    }

    private String resolveTitleByAction(String actionType) {
        return switch (actionType) {
            case ACTION_SLA_OVERDUE_SUBMIT -> "整改提交超时";
            case ACTION_SLA_OVERDUE_REVIEW -> "整改复核超时";
            case ACTION_SLA_ESCALATE_SUBMIT_L1 -> "整改提交超时升级（L1）";
            case ACTION_SLA_ESCALATE_SUBMIT_L2 -> "整改提交超时升级（L2）";
            case ACTION_SLA_ESCALATE_REVIEW_L1 -> "整改复核超时升级（L1）";
            case ACTION_SLA_ESCALATE_REVIEW_L2 -> "整改复核超时升级（L2）";
            default -> "整改SLA预警";
        };
    }

    private Map<String, Object> buildPayload(RectificationTask task, String actionType, LocalDateTime now) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("rectificationId", task.getId());
        payload.put("inspectionId", task.getInspectionId());
        payload.put("enterpriseId", task.getEnterpriseId());
        payload.put("status", task.getStatus() == null ? null : task.getStatus().name());
        payload.put("actionType", actionType);
        payload.put("scanTime", now);

        LocalDateTime deadline = task.getStatus() == RectificationStatus.SUBMITTED
            ? task.getReviewDeadline()
            : task.getSubmitDeadline();
        payload.put("deadline", deadline);
        if (deadline != null) {
            payload.put("overdueMinutes", Duration.between(deadline, now).toMinutes());
        }
        return payload;
    }

    private Long resolveRegionId(Long enterpriseId) {
        if (enterpriseId == null) {
            return null;
        }
        FoodEnterprise enterprise = foodEnterpriseMapper.selectOne(new LambdaQueryWrapper<FoodEnterprise>()
            .eq(FoodEnterprise::getId, enterpriseId)
            .eq(FoodEnterprise::getDeleted, 0)
            .last("limit 1"));
        return enterprise == null ? null : enterprise.getRegionId();
    }

    private Long resolveOwnerRegulatorId(Long inspectionId) {
        if (inspectionId == null) {
            return null;
        }
        InspectionRecord inspectionRecord = inspectionRecordMapper.selectOne(new LambdaQueryWrapper<InspectionRecord>()
            .eq(InspectionRecord::getId, inspectionId)
            .eq(InspectionRecord::getDeleted, 0)
            .last("limit 1"));
        return inspectionRecord == null ? null : inspectionRecord.getInspectorId();
    }

    private record StageSnapshot(long overdueMinutes,
                                 String overdueActionType,
                                 String escalateL1ActionType,
                                 String escalateL2ActionType,
                                 String overdueComment,
                                 String escalateL1Comment,
                                 String escalateL2Comment) {
    }
}
