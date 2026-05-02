package com.mortal.regulation.operation.service.impl;

import com.mortal.regulation.operation.client.regulation.vo.InternalEnterpriseDetailVO;
import com.mortal.regulation.operation.client.regulation.vo.InternalRegulatorIdentityVO;
import com.mortal.regulation.operation.common.OperationErrorMessages;
import com.mortal.regulation.operation.entity.InspectionTask;
import com.mortal.regulation.operation.entity.RectificationTask;
import com.mortal.regulation.operation.entity.SamplingResult;
import com.mortal.regulation.operation.entity.SamplingTask;
import com.mortal.regulation.operation.mapper.InspectionTaskMapper;
import com.mortal.regulation.operation.mapper.RectificationTaskMapper;
import com.mortal.regulation.operation.mapper.SamplingResultMapper;
import com.mortal.regulation.operation.mapper.SamplingTaskMapper;
import com.mortal.regulation.operation.service.AuditLogQueryService;
import com.mortal.regulation.operation.service.AuditLogService;
import com.mortal.regulation.operation.support.OperationMasterDataSupport;
import com.mortal.regulation.operation.vo.AuditLogVO;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuditLogQueryServiceImpl implements AuditLogQueryService {

    private static final String TARGET_INSPECTION_TASK = "INSPECTION_TASK";
    private static final String TARGET_SAMPLING_TASK = "SAMPLING_TASK";
    private static final String TARGET_SAMPLING_RESULT = "SAMPLING_RESULT";
    private static final String TARGET_RECTIFICATION_TASK = "RECTIFICATION_TASK";

    private final AuditLogService auditLogService;
    private final InspectionTaskMapper inspectionTaskMapper;
    private final SamplingTaskMapper samplingTaskMapper;
    private final SamplingResultMapper samplingResultMapper;
    private final RectificationTaskMapper rectificationTaskMapper;
    private final OperationMasterDataSupport masterDataSupport;

    public AuditLogQueryServiceImpl(AuditLogService auditLogService,
                                    InspectionTaskMapper inspectionTaskMapper,
                                    SamplingTaskMapper samplingTaskMapper,
                                    SamplingResultMapper samplingResultMapper,
                                    RectificationTaskMapper rectificationTaskMapper,
                                    OperationMasterDataSupport masterDataSupport) {
        this.auditLogService = auditLogService;
        this.inspectionTaskMapper = inspectionTaskMapper;
        this.samplingTaskMapper = samplingTaskMapper;
        this.samplingResultMapper = samplingResultMapper;
        this.rectificationTaskMapper = rectificationTaskMapper;
        this.masterDataSupport = masterDataSupport;
    }

    @Override
    public List<AuditLogVO> listLogs(Long operatorUserId, String operatorUserType, String targetType, Long targetId, int limit) {
        if (!StringUtils.hasText(targetType) || targetId == null) {
            return List.of();
        }
        ensureTargetVisible(operatorUserId, operatorUserType, targetType, targetId);
        return auditLogService.listTargetLogs(normalizeTargetType(targetType), targetId, limit);
    }

    @Override
    public List<AuditLogVO> listRecentLogs(Long operatorUserId, String operatorUserType, String bizType, int limit) {
        int expectedSize = normalizeLimit(limit);
        int fetchSize = Math.min(Math.max(expectedSize * 5, 20), 200);
        return auditLogService.listRecentLogs(bizType, fetchSize).stream()
            .filter(log -> isTargetVisible(operatorUserId, operatorUserType, log.getTargetType(), log.getTargetId()))
            .limit(expectedSize)
            .toList();
    }

    private void ensureTargetVisible(Long operatorUserId, String operatorUserType, String targetType, Long targetId) {
        if (!isTargetVisible(operatorUserId, operatorUserType, targetType, targetId)) {
            throw new IllegalArgumentException(OperationErrorMessages.NOT_IN_SCOPE);
        }
    }

    private boolean isTargetVisible(Long operatorUserId, String operatorUserType, String targetType, Long targetId) {
        String normalizedTargetType = normalizeTargetType(targetType);
        if (!StringUtils.hasText(operatorUserType) || operatorUserId == null || normalizedTargetType == null || targetId == null) {
            return false;
        }
        if ("ENTERPRISE".equalsIgnoreCase(operatorUserType)) {
            InternalEnterpriseDetailVO enterprise = masterDataSupport.requireEnterpriseByUserId(operatorUserId);
            return isTargetVisibleToEnterprise(enterprise.getId(), normalizedTargetType, targetId);
        }
        if ("REGULATOR".equalsIgnoreCase(operatorUserType) || "ADMIN".equalsIgnoreCase(operatorUserType)) {
            InternalRegulatorIdentityVO regulator = masterDataSupport.requireRegulatorByUserId(operatorUserId);
            return isTargetVisibleToRegulator(regulator, normalizedTargetType, targetId);
        }
        return false;
    }

    private boolean isTargetVisibleToEnterprise(Long enterpriseId, String targetType, Long targetId) {
        if (enterpriseId == null) {
            return false;
        }
        return switch (targetType) {
            case TARGET_INSPECTION_TASK -> {
                InspectionTask task = requireInspectionTask(targetId);
                yield Objects.equals(task.getEnterpriseId(), enterpriseId);
            }
            case TARGET_SAMPLING_TASK -> {
                SamplingTask task = requireSamplingTask(targetId);
                yield Objects.equals(task.getEnterpriseId(), enterpriseId);
            }
            case TARGET_SAMPLING_RESULT -> {
                SamplingResult result = requireSamplingResult(targetId);
                yield Objects.equals(result.getEnterpriseId(), enterpriseId);
            }
            case TARGET_RECTIFICATION_TASK -> {
                RectificationTask task = requireRectificationTask(targetId);
                yield Objects.equals(task.getEnterpriseId(), enterpriseId);
            }
            default -> false;
        };
    }

    private boolean isTargetVisibleToRegulator(InternalRegulatorIdentityVO regulator, String targetType, Long targetId) {
        if (regulator == null || regulator.getId() == null) {
            return false;
        }
        boolean isEnforcer = OperationMasterDataSupport.ROLE_ENFORCER.equalsIgnoreCase(regulator.getRoleType());
        Long regulatorId = regulator.getId();
        return switch (targetType) {
            case TARGET_INSPECTION_TASK -> {
                InspectionTask task = requireInspectionTask(targetId);
                if (!masterDataSupport.isEnterpriseInRegulatorScope(regulatorId, task.getEnterpriseId())) {
                    yield false;
                }
                yield !isEnforcer || Objects.equals(task.getAssignedTo(), regulatorId);
            }
            case TARGET_SAMPLING_TASK -> {
                SamplingTask task = requireSamplingTask(targetId);
                if (!masterDataSupport.isEnterpriseInRegulatorScope(regulatorId, task.getEnterpriseId())) {
                    yield false;
                }
                yield !isEnforcer || Objects.equals(task.getAssignedTo(), regulatorId);
            }
            case TARGET_SAMPLING_RESULT -> {
                SamplingResult result = requireSamplingResult(targetId);
                if (!masterDataSupport.isEnterpriseInRegulatorScope(regulatorId, result.getEnterpriseId())) {
                    yield false;
                }
                yield !isEnforcer || Objects.equals(result.getSampledBy(), regulatorId);
            }
            case TARGET_RECTIFICATION_TASK -> {
                RectificationTask task = requireRectificationTask(targetId);
                yield masterDataSupport.isEnterpriseInRegulatorScope(regulatorId, task.getEnterpriseId());
            }
            default -> false;
        };
    }

    private String normalizeTargetType(String targetType) {
        if (!StringUtils.hasText(targetType)) {
            return null;
        }
        String normalized = targetType.trim().toUpperCase();
        return switch (normalized) {
            case TARGET_INSPECTION_TASK, TARGET_SAMPLING_TASK, TARGET_SAMPLING_RESULT, TARGET_RECTIFICATION_TASK -> normalized;
            default -> null;
        };
    }

    private InspectionTask requireInspectionTask(Long targetId) {
        InspectionTask task = inspectionTaskMapper.selectById(targetId);
        if (task == null || isDeleted(task.getDeleted())) {
            throw new IllegalArgumentException("inspection task not found");
        }
        return task;
    }

    private SamplingTask requireSamplingTask(Long targetId) {
        SamplingTask task = samplingTaskMapper.selectById(targetId);
        if (task == null || isDeleted(task.getDeleted())) {
            throw new IllegalArgumentException("sampling task not found");
        }
        return task;
    }

    private SamplingResult requireSamplingResult(Long targetId) {
        SamplingResult result = samplingResultMapper.selectById(targetId);
        if (result == null || isDeleted(result.getDeleted())) {
            throw new IllegalArgumentException("sampling result not found");
        }
        return result;
    }

    private RectificationTask requireRectificationTask(Long targetId) {
        RectificationTask task = rectificationTaskMapper.selectById(targetId);
        if (task == null || isDeleted(task.getDeleted())) {
            throw new IllegalArgumentException("rectification task not found");
        }
        return task;
    }

    private int normalizeLimit(int limit) {
        return limit <= 0 ? 10 : Math.min(limit, 50);
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}
