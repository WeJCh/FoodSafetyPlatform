package com.mortal.regulation.service.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.regulation.common.enums.RectificationStatus;
import com.mortal.regulation.entity.RectificationActionLog;
import com.mortal.regulation.entity.RectificationTask;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 整改任务 SLA 定时扫描器（轻量版）。
 * <p>
 * 当前版本只做两件事：
 * 1) 记录超时事件；
 * 2) 记录升级提醒事件（L1/L2）。
 * </p>
 */
@Component
public class RectificationSlaScheduler {

    private static final Logger log = LoggerFactory.getLogger(RectificationSlaScheduler.class);

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
                                     RectificationActionLogMapper rectificationActionLogMapper) {
        this.rectificationTaskMapper = rectificationTaskMapper;
        this.rectificationActionLogMapper = rectificationActionLogMapper;
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
        ensureActionLog(task.getId(), snapshot.overdueActionType, snapshot.overdueComment, loggedActions);
        if (snapshot.overdueMinutes >= escalateL1Minutes) {
            ensureActionLog(task.getId(), snapshot.escalateL1ActionType, snapshot.escalateL1Comment, loggedActions);
        }
        if (snapshot.overdueMinutes >= escalateL2Minutes) {
            ensureActionLog(task.getId(), snapshot.escalateL2ActionType, snapshot.escalateL2Comment, loggedActions);
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

    private void ensureActionLog(Long taskId,
                                 String actionType,
                                 String comment,
                                 Map<Long, Set<String>> loggedActions) {
        if (taskId == null || actionType == null) {
            return;
        }
        Set<String> actionTypes = loggedActions.computeIfAbsent(taskId, key -> new HashSet<>());
        if (actionTypes.contains(actionType)) {
            return;
        }
        RectificationActionLog logItem = new RectificationActionLog();
        logItem.setRectificationId(taskId);
        logItem.setActionType(actionType);
        logItem.setOperatorId(null);
        logItem.setActionComment(comment);
        logItem.setAttachmentUrls(null);
        logItem.setCreateTime(LocalDateTime.now());
        logItem.setDeleted(0);
        rectificationActionLogMapper.insert(logItem);
        actionTypes.add(actionType);
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
