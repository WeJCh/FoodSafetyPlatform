package com.mortal.regulation.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.operation.client.regulation.vo.InternalEnterpriseDetailVO;
import com.mortal.regulation.operation.client.regulation.vo.InternalRegulatorIdentityVO;
import com.mortal.regulation.operation.common.OperationErrorMessages;
import com.mortal.regulation.operation.common.enums.RectificationReviewAction;
import com.mortal.regulation.operation.common.enums.RectificationStatus;
import com.mortal.regulation.operation.dto.RectificationReviewDTO;
import com.mortal.regulation.operation.dto.RectificationSubmitDTO;
import com.mortal.regulation.operation.entity.RectificationActionLog;
import com.mortal.regulation.operation.entity.RectificationTask;
import com.mortal.regulation.operation.mapper.RectificationActionLogMapper;
import com.mortal.regulation.operation.mapper.RectificationTaskMapper;
import com.mortal.regulation.operation.service.RectificationService;
import com.mortal.regulation.operation.service.StatusTransitionValidator;
import com.mortal.regulation.operation.support.OperationLockSupport;
import com.mortal.regulation.operation.support.OperationMasterDataSupport;
import com.mortal.regulation.operation.vo.RectificationActionLogVO;
import com.mortal.regulation.operation.vo.RectificationTaskVO;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RectificationServiceImpl implements RectificationService {

    private static final String DEFAULT_RECTIFICATION_DESC = "请根据检查问题完成整改并提交说明";
    private static final String ACTION_SYSTEM_CREATE = "SYSTEM_CREATE";
    private static final String ACTION_ENTERPRISE_SUBMIT = "ENTERPRISE_SUBMIT";
    private static final String ACTION_REVIEW_CONFIRM = "REVIEW_CONFIRM";
    private static final String ACTION_REVIEW_REWORK = "REVIEW_REWORK";
    private static final long ENTERPRISE_SUBMIT_DEADLINE_HOURS = 72L;
    private static final long ENTERPRISE_RESUBMIT_DEADLINE_HOURS = 48L;
    private static final long REGULATOR_REVIEW_DEADLINE_HOURS = 24L;
    private static final long SLA_DUE_SOON_MINUTES = 24L * 60L;

    private final RectificationTaskMapper rectificationTaskMapper;
    private final RectificationActionLogMapper rectificationActionLogMapper;
    private final OperationMasterDataSupport masterDataSupport;
    private final OperationLockSupport operationLockSupport;
    private final ObjectMapper objectMapper;

    public RectificationServiceImpl(RectificationTaskMapper rectificationTaskMapper,
                                    RectificationActionLogMapper rectificationActionLogMapper,
                                    OperationMasterDataSupport masterDataSupport,
                                    OperationLockSupport operationLockSupport,
                                    ObjectMapper objectMapper) {
        this.rectificationTaskMapper = rectificationTaskMapper;
        this.rectificationActionLogMapper = rectificationActionLogMapper;
        this.masterDataSupport = masterDataSupport;
        this.operationLockSupport = operationLockSupport;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFromInspection(Long inspectionId, Long enterpriseId, String rectificationDesc) {
        if (inspectionId == null || enterpriseId == null) {
            return;
        }
        RectificationTask existing = rectificationTaskMapper.selectOne(new LambdaQueryWrapper<RectificationTask>()
            .eq(RectificationTask::getInspectionId, inspectionId)
            .eq(RectificationTask::getDeleted, 0)
            .last("limit 1"));
        if (existing != null) {
            return;
        }
        RectificationTask task = new RectificationTask();
        task.setInspectionId(inspectionId);
        task.setEnterpriseId(enterpriseId);
        task.setRectificationDesc(normalizeRectificationDesc(rectificationDesc));
        task.setStatus(RectificationStatus.ONGOING);
        LocalDateTime now = LocalDateTime.now();
        task.setSubmitDeadline(now.plusHours(ENTERPRISE_SUBMIT_DEADLINE_HOURS));
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setDeleted(0);
        try {
            rectificationTaskMapper.insert(task);
            saveActionLog(task.getId(), ACTION_SYSTEM_CREATE, null, task.getRectificationDesc(), null);
        } catch (DuplicateKeyException ignored) {
            // ignore duplicate create caused by concurrent submit
        }
    }

    @Override
    public PageResult<RectificationTaskVO> listMy(Long enterpriseUserId, String status, String slaFilter, int page, int size) {
        InternalEnterpriseDetailVO enterprise = masterDataSupport.requireEnterpriseByUserId(enterpriseUserId);
        LambdaQueryWrapper<RectificationTask> wrapper = new LambdaQueryWrapper<RectificationTask>()
            .eq(RectificationTask::getDeleted, 0)
            .eq(RectificationTask::getEnterpriseId, enterprise.getId());
        RectificationStatus statusValue = normalizeStatus(status);
        if (statusValue != null) {
            wrapper.eq(RectificationTask::getStatus, statusValue);
        }
        applyEnterpriseSlaFilter(wrapper, slaFilter);
        wrapper.orderByDesc(RectificationTask::getUpdateTime);
        Page<RectificationTask> pageInfo = rectificationTaskMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(toVOs(pageInfo.getRecords()), pageInfo.getTotal(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RectificationTaskVO submitMy(Long enterpriseUserId, Long rectificationId, RectificationSubmitDTO dto) {
        return operationLockSupport.executeWithLock(
            "rectification-submit",
            rectificationId,
            () -> doSubmitMy(enterpriseUserId, rectificationId, dto)
        );
    }

    private RectificationTaskVO doSubmitMy(Long enterpriseUserId, Long rectificationId, RectificationSubmitDTO dto) {
        InternalEnterpriseDetailVO enterprise = masterDataSupport.requireEnterpriseByUserId(enterpriseUserId);
        RectificationTask task = requireTask(rectificationId);
        if (!Objects.equals(task.getEnterpriseId(), enterprise.getId())) {
            throw new IllegalArgumentException(OperationErrorMessages.RECTIFICATION_NOT_FOUND);
        }
        StatusTransitionValidator.validateRectificationTransition(task.getStatus(), RectificationStatus.SUBMITTED);
        String progress = dto.getProgress().trim();
        LocalDateTime now = LocalDateTime.now();
        task.setProgress(progress);
        task.setStatus(RectificationStatus.SUBMITTED);
        task.setFinishTime(now);
        task.setReviewDeadline(now.plusHours(REGULATOR_REVIEW_DEADLINE_HOURS));
        task.setUpdateTime(now);
        rectificationTaskMapper.updateById(task);
        saveActionLog(task.getId(), ACTION_ENTERPRISE_SUBMIT, enterpriseUserId, progress, dto.getAttachmentUrls());
        return toVOWithNames(task);
    }

    @Override
    public PageResult<RectificationTaskVO> listForAdmin(Long regulatorUserId,
                                                        String status,
                                                        String enterpriseName,
                                                        int page,
                                                        int size) {
        InternalRegulatorIdentityVO regulator = masterDataSupport.requireAdmin(regulatorUserId);
        return listForRegulatorRole(regulator.getId(), status, enterpriseName, page, size);
    }

    @Override
    public PageResult<RectificationTaskVO> listForEnforcer(Long regulatorUserId,
                                                           String status,
                                                           String enterpriseName,
                                                           int page,
                                                           int size) {
        InternalRegulatorIdentityVO regulator = masterDataSupport.requireEnforcer(regulatorUserId);
        return listForRegulatorRole(regulator.getId(), status, enterpriseName, page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RectificationTaskVO review(Long regulatorUserId, Long rectificationId, RectificationReviewDTO dto) {
        return operationLockSupport.executeWithLock(
            "rectification-review",
            rectificationId,
            () -> doReview(regulatorUserId, rectificationId, dto)
        );
    }

    private RectificationTaskVO doReview(Long regulatorUserId, Long rectificationId, RectificationReviewDTO dto) {
        InternalRegulatorIdentityVO regulator = masterDataSupport.requireAdmin(regulatorUserId);
        RectificationTask task = requireTask(rectificationId);
        masterDataSupport.requireEnterpriseInScope(regulator.getId(), task.getEnterpriseId());
        RectificationReviewAction action = normalizeReviewAction(dto.getAction());
        String comment = normalizeOptionalText(dto.getComment());
        if (action == RectificationReviewAction.REWORK && !StringUtils.hasText(comment)) {
            throw new IllegalArgumentException("review comment required for rework");
        }
        LocalDateTime now = LocalDateTime.now();
        if (action == RectificationReviewAction.CONFIRM) {
            StatusTransitionValidator.validateRectificationTransition(task.getStatus(), RectificationStatus.CONFIRMED);
            task.setStatus(RectificationStatus.CONFIRMED);
            task.setConfirmedBy(regulator.getId());
            task.setConfirmedTime(now);
        } else {
            StatusTransitionValidator.validateRectificationTransition(task.getStatus(), RectificationStatus.REWORK);
            task.setStatus(RectificationStatus.REWORK);
            task.setConfirmedBy(null);
            task.setConfirmedTime(null);
            task.setSubmitDeadline(now.plusHours(ENTERPRISE_RESUBMIT_DEADLINE_HOURS));
            task.setReviewDeadline(null);
        }
        task.setUpdateTime(now);
        rectificationTaskMapper.updateById(task);
        saveActionLog(
            task.getId(),
            action == RectificationReviewAction.CONFIRM ? ACTION_REVIEW_CONFIRM : ACTION_REVIEW_REWORK,
            regulatorUserId,
            comment,
            dto.getAttachmentUrls()
        );
        return toVOWithNames(task);
    }

    @Override
    public RectificationTaskVO getDetail(Long operatorUserId, String userType, Long rectificationId) {
        RectificationTask task = resolveVisibleTask(operatorUserId, userType, rectificationId);
        return toVOWithNames(task);
    }

    @Override
    public List<RectificationActionLogVO> listActions(Long operatorUserId, String userType, Long rectificationId) {
        RectificationTask task = resolveVisibleTask(operatorUserId, userType, rectificationId);
        List<RectificationActionLog> logs = rectificationActionLogMapper.selectList(new LambdaQueryWrapper<RectificationActionLog>()
            .eq(RectificationActionLog::getRectificationId, task.getId())
            .eq(RectificationActionLog::getDeleted, 0)
            .orderByAsc(RectificationActionLog::getCreateTime, RectificationActionLog::getId));
        if (logs.isEmpty()) {
            return List.of();
        }
        Map<Long, String> operatorNames = loadActionOperatorNames(logs);
        return logs.stream().map(log -> toActionLogVO(log, operatorNames)).toList();
    }

    private PageResult<RectificationTaskVO> listForRegulatorRole(Long regulatorId,
                                                                 String status,
                                                                 String enterpriseName,
                                                                 int page,
                                                                 int size) {
        List<Long> enterpriseIds = masterDataSupport.resolveScopedEnterpriseIds(regulatorId, enterpriseName);
        if (enterpriseIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        LambdaQueryWrapper<RectificationTask> wrapper = new LambdaQueryWrapper<RectificationTask>()
            .eq(RectificationTask::getDeleted, 0)
            .in(RectificationTask::getEnterpriseId, enterpriseIds);
        RectificationStatus statusValue = normalizeStatus(status);
        if (statusValue != null) {
            wrapper.eq(RectificationTask::getStatus, statusValue);
        }
        wrapper.orderByDesc(RectificationTask::getUpdateTime);
        Page<RectificationTask> pageInfo = rectificationTaskMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(toVOs(pageInfo.getRecords()), pageInfo.getTotal(), page, size);
    }

    private void applyEnterpriseSlaFilter(LambdaQueryWrapper<RectificationTask> wrapper, String slaFilter) {
        String normalized = normalizeSlaFilter(slaFilter);
        if (normalized == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if ("OVERDUE".equals(normalized)) {
            wrapper.and(group -> group
                .and(submit -> submit
                    .in(RectificationTask::getStatus, RectificationStatus.ONGOING, RectificationStatus.REWORK)
                    .isNotNull(RectificationTask::getSubmitDeadline)
                    .lt(RectificationTask::getSubmitDeadline, now)
                )
                .or(review -> review
                    .eq(RectificationTask::getStatus, RectificationStatus.SUBMITTED)
                    .isNotNull(RectificationTask::getReviewDeadline)
                    .lt(RectificationTask::getReviewDeadline, now)
                )
            );
            return;
        }
        if ("NOT_OVERDUE".equals(normalized)) {
            wrapper.and(group -> group
                .and(submit -> submit
                    .in(RectificationTask::getStatus, RectificationStatus.ONGOING, RectificationStatus.REWORK)
                    .and(active -> active
                        .isNull(RectificationTask::getSubmitDeadline)
                        .or()
                        .ge(RectificationTask::getSubmitDeadline, now)
                    )
                )
                .or(review -> review
                    .eq(RectificationTask::getStatus, RectificationStatus.SUBMITTED)
                    .and(active -> active
                        .isNull(RectificationTask::getReviewDeadline)
                        .or()
                        .ge(RectificationTask::getReviewDeadline, now)
                    )
                )
                .or(closed -> closed.notIn(
                    RectificationTask::getStatus,
                    RectificationStatus.ONGOING,
                    RectificationStatus.REWORK,
                    RectificationStatus.SUBMITTED
                ))
            );
            return;
        }
        if ("AT_RISK".equals(normalized)) {
            LocalDateTime dueSoonThreshold = now.plusMinutes(SLA_DUE_SOON_MINUTES);
            wrapper.and(group -> group
                .and(submit -> submit
                    .in(RectificationTask::getStatus, RectificationStatus.ONGOING, RectificationStatus.REWORK)
                    .isNotNull(RectificationTask::getSubmitDeadline)
                    .le(RectificationTask::getSubmitDeadline, dueSoonThreshold)
                )
                .or(review -> review
                    .eq(RectificationTask::getStatus, RectificationStatus.SUBMITTED)
                    .isNotNull(RectificationTask::getReviewDeadline)
                    .le(RectificationTask::getReviewDeadline, dueSoonThreshold)
                )
            );
        }
    }

    private RectificationTask resolveVisibleTask(Long operatorUserId, String userType, Long rectificationId) {
        RectificationTask task = requireTask(rectificationId);
        if ("ENTERPRISE".equalsIgnoreCase(userType)) {
            InternalEnterpriseDetailVO enterprise = masterDataSupport.requireEnterpriseByUserId(operatorUserId);
            if (!Objects.equals(task.getEnterpriseId(), enterprise.getId())) {
                throw new IllegalArgumentException(OperationErrorMessages.RECTIFICATION_NOT_FOUND);
            }
            return task;
        }
        if ("REGULATOR".equalsIgnoreCase(userType) || "ADMIN".equalsIgnoreCase(userType)) {
            InternalRegulatorIdentityVO regulator = masterDataSupport.requireRegulatorByUserId(operatorUserId);
            masterDataSupport.requireEnterpriseInScope(regulator.getId(), task.getEnterpriseId());
            return task;
        }
        throw new IllegalArgumentException(OperationErrorMessages.UNAUTHORIZED);
    }

    private RectificationTask requireTask(Long rectificationId) {
        RectificationTask task = rectificationTaskMapper.selectById(rectificationId);
        if (task == null || isDeleted(task.getDeleted())) {
            throw new IllegalArgumentException(OperationErrorMessages.RECTIFICATION_NOT_FOUND);
        }
        return task;
    }

    private List<RectificationTaskVO> toVOs(List<RectificationTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        Map<Long, String> enterpriseNames = loadEnterpriseNames(tasks);
        Map<Long, String> regulatorNames = loadRegulatorNames(tasks);
        LocalDateTime now = LocalDateTime.now();
        return tasks.stream().map(task -> toVO(task, enterpriseNames, regulatorNames, now)).toList();
    }

    private RectificationTaskVO toVOWithNames(RectificationTask task) {
        Map<Long, String> enterpriseNames = loadEnterpriseNames(List.of(task));
        Map<Long, String> regulatorNames = loadRegulatorNames(List.of(task));
        return toVO(task, enterpriseNames, regulatorNames, LocalDateTime.now());
    }

    private RectificationTaskVO toVO(RectificationTask task,
                                     Map<Long, String> enterpriseNames,
                                     Map<Long, String> regulatorNames,
                                     LocalDateTime now) {
        RectificationTaskVO vo = new RectificationTaskVO();
        vo.setId(task.getId());
        vo.setInspectionId(task.getInspectionId());
        vo.setEnterpriseId(task.getEnterpriseId());
        vo.setEnterpriseName(enterpriseNames.get(task.getEnterpriseId()));
        vo.setRectificationDesc(task.getRectificationDesc());
        vo.setProgress(task.getProgress());
        vo.setStatus(task.getStatus());
        vo.setSubmitDeadline(task.getSubmitDeadline());
        vo.setReviewDeadline(task.getReviewDeadline());
        vo.setFinishTime(task.getFinishTime());
        vo.setConfirmedBy(task.getConfirmedBy());
        vo.setConfirmedByName(regulatorNames.get(task.getConfirmedBy()));
        vo.setConfirmedTime(task.getConfirmedTime());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        fillSlaSnapshot(vo, task, now);
        return vo;
    }

    private void fillSlaSnapshot(RectificationTaskVO vo, RectificationTask task, LocalDateTime now) {
        if (task == null || task.getStatus() == null) {
            vo.setSlaStage("NONE");
            vo.setSlaStatus("NONE");
            return;
        }

        LocalDateTime activeDeadline;
        String stage;
        if (task.getStatus() == RectificationStatus.ONGOING || task.getStatus() == RectificationStatus.REWORK) {
            activeDeadline = task.getSubmitDeadline();
            stage = "ENTERPRISE_SUBMIT";
        } else if (task.getStatus() == RectificationStatus.SUBMITTED) {
            activeDeadline = task.getReviewDeadline();
            stage = "REGULATOR_REVIEW";
        } else {
            activeDeadline = null;
            stage = "NONE";
        }

        vo.setSlaStage(stage);
        vo.setCurrentDeadline(activeDeadline);
        if (activeDeadline == null || "NONE".equals(stage)) {
            vo.setSlaStatus("NONE");
            vo.setRemainingMinutes(null);
            return;
        }

        long remainingMinutes = Duration.between(now, activeDeadline).toMinutes();
        vo.setRemainingMinutes(remainingMinutes);
        if (remainingMinutes < 0) {
            vo.setSlaStatus("OVERDUE");
        } else if (remainingMinutes <= SLA_DUE_SOON_MINUTES) {
            vo.setSlaStatus("DUE_SOON");
        } else {
            vo.setSlaStatus("NORMAL");
        }
    }

    private RectificationActionLogVO toActionLogVO(RectificationActionLog log, Map<Long, String> operatorNames) {
        RectificationActionLogVO vo = new RectificationActionLogVO();
        vo.setId(log.getId());
        vo.setRectificationId(log.getRectificationId());
        vo.setActionType(log.getActionType());
        vo.setActionName(resolveActionDisplayName(log.getActionType()));
        vo.setOperatorId(log.getOperatorId());
        vo.setOperatorName(operatorNames.get(log.getOperatorId()));
        vo.setActionComment(log.getActionComment());
        vo.setAttachmentUrls(parseAttachmentUrls(log.getAttachmentUrls()));
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    private String resolveActionDisplayName(String actionType) {
        if (!StringUtils.hasText(actionType)) {
            return "unknown action";
        }
        return switch (actionType.trim().toUpperCase()) {
            case ACTION_SYSTEM_CREATE -> "系统创建整改任务";
            case ACTION_ENTERPRISE_SUBMIT -> "企业提交整改";
            case ACTION_REVIEW_CONFIRM -> "监管复核通过";
            case ACTION_REVIEW_REWORK -> "监管打回重做";
            case "SLA_OVERDUE_SUBMIT" -> "企业提交超时";
            case "SLA_OVERDUE_REVIEW" -> "监管复核超时";
            default -> actionType;
        };
    }

    private Map<Long, String> loadActionOperatorNames(List<RectificationActionLog> logs) {
        Set<Long> userIds = logs.stream()
            .map(RectificationActionLog::getOperatorId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return masterDataSupport.loadOperatorNamesByUserIds(userIds);
    }

    private Map<Long, String> loadEnterpriseNames(List<RectificationTask> tasks) {
        List<Long> enterpriseIds = tasks.stream()
            .map(RectificationTask::getEnterpriseId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (enterpriseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return masterDataSupport.loadEnterpriseNames(enterpriseIds);
    }

    private Map<Long, String> loadRegulatorNames(List<RectificationTask> tasks) {
        Set<Long> regulatorIds = tasks.stream()
            .map(RectificationTask::getConfirmedBy)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (regulatorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return masterDataSupport.loadRegulatorNames(regulatorIds);
    }

    private RectificationStatus normalizeStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return RectificationStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid rectification status");
        }
    }

    private String normalizeSlaFilter(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if ("OVERDUE".equals(normalized) || "NOT_OVERDUE".equals(normalized) || "AT_RISK".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private RectificationReviewAction normalizeReviewAction(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("review action required");
        }
        try {
            return RectificationReviewAction.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid review action");
        }
    }

    private List<String> parseAttachmentUrls(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        try {
            List<String> urls = objectMapper.readValue(value, new TypeReference<List<String>>() {
            });
            if (urls == null || urls.isEmpty()) {
                return List.of();
            }
            return urls.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String serializeAttachmentUrls(List<String> attachmentUrls) {
        if (attachmentUrls == null || attachmentUrls.isEmpty()) {
            return null;
        }
        List<String> normalized = new ArrayList<>();
        for (String attachmentUrl : attachmentUrls) {
            if (StringUtils.hasText(attachmentUrl)) {
                normalized.add(attachmentUrl.trim());
            }
        }
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("invalid attachment urls");
        }
    }

    private void saveActionLog(Long rectificationId,
                               String actionType,
                               Long operatorId,
                               String actionComment,
                               List<String> attachmentUrls) {
        RectificationActionLog log = new RectificationActionLog();
        log.setRectificationId(rectificationId);
        log.setActionType(actionType);
        log.setOperatorId(operatorId);
        log.setActionComment(normalizeOptionalText(actionComment));
        log.setAttachmentUrls(serializeAttachmentUrls(attachmentUrls));
        log.setCreateTime(LocalDateTime.now());
        log.setDeleted(0);
        rectificationActionLogMapper.insert(log);
    }

    private String normalizeRectificationDesc(String value) {
        if (!StringUtils.hasText(value)) {
            return DEFAULT_RECTIFICATION_DESC;
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}
