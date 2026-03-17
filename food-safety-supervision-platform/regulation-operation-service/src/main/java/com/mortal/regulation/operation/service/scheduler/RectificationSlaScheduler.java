package com.mortal.regulation.operation.service.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.regulation.operation.common.enums.RectificationStatus;
import com.mortal.regulation.operation.dto.WarningEventUpsertDTO;
import com.mortal.regulation.operation.entity.InspectionRecord;
import com.mortal.regulation.operation.entity.InspectionTask;
import com.mortal.regulation.operation.entity.RectificationActionLog;
import com.mortal.regulation.operation.entity.RectificationTask;
import com.mortal.regulation.operation.mapper.InspectionRecordMapper;
import com.mortal.regulation.operation.mapper.InspectionTaskMapper;
import com.mortal.regulation.operation.mapper.RectificationActionLogMapper;
import com.mortal.regulation.operation.mapper.RectificationTaskMapper;
import com.mortal.regulation.operation.service.WarningEventOutboxService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RectificationSlaScheduler {

    private static final Logger log = LoggerFactory.getLogger(RectificationSlaScheduler.class);

    private static final String BIZ_TYPE_RECTIFICATION = "RECTIFICATION";
    private static final String SOURCE_SERVICE = "regulation-operation-service";

    private static final String ACTION_SLA_OVERDUE_SUBMIT = "SLA_OVERDUE_SUBMIT";
    private static final String ACTION_SLA_OVERDUE_REVIEW = "SLA_OVERDUE_REVIEW";

    private static final Set<String> SLA_ACTION_TYPES = Set.of(
        ACTION_SLA_OVERDUE_SUBMIT,
        ACTION_SLA_OVERDUE_REVIEW
    );

    private final RectificationTaskMapper rectificationTaskMapper;
    private final RectificationActionLogMapper rectificationActionLogMapper;
    private final WarningEventOutboxService warningEventOutboxService;
    private final InspectionRecordMapper inspectionRecordMapper;
    private final InspectionTaskMapper inspectionTaskMapper;

    public RectificationSlaScheduler(RectificationTaskMapper rectificationTaskMapper,
                                     RectificationActionLogMapper rectificationActionLogMapper,
                                     WarningEventOutboxService warningEventOutboxService,
                                     InspectionRecordMapper inspectionRecordMapper,
                                     InspectionTaskMapper inspectionTaskMapper) {
        this.rectificationTaskMapper = rectificationTaskMapper;
        this.rectificationActionLogMapper = rectificationActionLogMapper;
        this.warningEventOutboxService = warningEventOutboxService;
        this.inspectionRecordMapper = inspectionRecordMapper;
        this.inspectionTaskMapper = inspectionTaskMapper;
    }

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
        ensureActionLogAndWarning(task, snapshot.actionType(), snapshot.comment(), loggedActions, now);
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
                ACTION_SLA_OVERDUE_SUBMIT,
                "企业整改提交已超时"
            );
        }

        if (status == RectificationStatus.SUBMITTED) {
            long overdueMinutes = minutesOverdue(task.getReviewDeadline(), now);
            if (overdueMinutes <= 0) {
                return null;
            }
            return new StageSnapshot(
                ACTION_SLA_OVERDUE_REVIEW,
                "监管复核已超时"
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

        WarningEventUpsertDTO dto = buildWarningEventDto(task, actionType, comment, now);
        warningEventOutboxService.ensurePendingEvent(dedupKey, dto, now);
        boolean pushed = warningEventOutboxService.dispatchByEventKey(dedupKey);
        if (!pushed) {
            log.warn("Warning outbox dispatch deferred. taskId={}, actionType={}, eventKey={}",
                taskId, actionType, dedupKey);
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
        dto.setRegionId(resolveRegionId(task.getInspectionId()));
        dto.setOwnerRegulatorId(resolveOwnerRegulatorId(task.getInspectionId()));
        dto.setDedupKey(buildDedupKey(task.getId(), actionType));
        dto.setLevel("L1");
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

    private String resolveTitleByAction(String actionType) {
        return switch (actionType) {
            case ACTION_SLA_OVERDUE_SUBMIT -> "整改提交超时";
            case ACTION_SLA_OVERDUE_REVIEW -> "整改复核超时";
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

    private Long resolveRegionId(Long inspectionId) {
        InspectionRecord inspectionRecord = loadInspectionRecord(inspectionId);
        if (inspectionRecord == null || inspectionRecord.getTaskId() == null) {
            return null;
        }
        InspectionTask inspectionTask = inspectionTaskMapper.selectOne(new LambdaQueryWrapper<InspectionTask>()
            .eq(InspectionTask::getId, inspectionRecord.getTaskId())
            .eq(InspectionTask::getDeleted, 0)
            .last("limit 1"));
        return inspectionTask == null ? null : inspectionTask.getRegionId();
    }

    private Long resolveOwnerRegulatorId(Long inspectionId) {
        InspectionRecord inspectionRecord = loadInspectionRecord(inspectionId);
        return inspectionRecord == null ? null : inspectionRecord.getInspectorId();
    }

    private InspectionRecord loadInspectionRecord(Long inspectionId) {
        if (inspectionId == null) {
            return null;
        }
        return inspectionRecordMapper.selectOne(new LambdaQueryWrapper<InspectionRecord>()
            .eq(InspectionRecord::getId, inspectionId)
            .eq(InspectionRecord::getDeleted, 0)
            .last("limit 1"));
    }

    private record StageSnapshot(String actionType, String comment) {
    }
}
