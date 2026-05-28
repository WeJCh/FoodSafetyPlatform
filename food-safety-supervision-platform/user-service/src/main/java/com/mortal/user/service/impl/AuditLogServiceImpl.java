package com.mortal.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.user.entity.AuditLog;
import com.mortal.user.mapper.AuditLogMapper;
import com.mortal.user.service.AuditLogService;
import com.mortal.user.vo.AuditLogVO;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final String SERVICE_NAME = "user-service";
    private static final String SYSTEM_OPERATOR_NAME = "\u7CFB\u7EDF";

    private final AuditLogMapper auditLogMapper;

    public AuditLogServiceImpl(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public void recordAudit(Long operatorUserId,
                            String operatorUserType,
                            String operatorName,
                            String targetType,
                            Long targetId,
                            Long targetUserId,
                            String targetName,
                            String bizType,
                            String actionType,
                            String actionName,
                            String beforeData,
                            String afterData,
                            String remark) {
        recordAudit(
            operatorUserId,
            operatorUserType,
            operatorName,
            targetType,
            targetId,
            targetUserId,
            targetName,
            bizType,
            actionType,
            actionName,
            beforeData,
            afterData,
            1,
            null,
            remark
        );
    }

    @Override
    public void recordAudit(Long operatorUserId,
                            String operatorUserType,
                            String operatorName,
                            String targetType,
                            Long targetId,
                            Long targetUserId,
                            String targetName,
                            String bizType,
                            String actionType,
                            String actionName,
                            String beforeData,
                            String afterData,
                            Integer successFlag,
                            String errorMessage,
                            String remark) {
        if (!StringUtils.hasText(targetType) || targetId == null || !StringUtils.hasText(bizType)
            || !StringUtils.hasText(actionType)) {
            return;
        }
        AuditLog log = new AuditLog();
        log.setServiceName(SERVICE_NAME);
        log.setOperatorUserId(operatorUserId);
        log.setOperatorUserType(trimToNull(operatorUserType));
        log.setOperatorName(StringUtils.hasText(operatorName) ? operatorName.trim() : SYSTEM_OPERATOR_NAME);
        log.setTargetType(targetType.trim());
        log.setTargetId(targetId);
        log.setTargetUserId(targetUserId);
        log.setTargetName(trimToNull(targetName));
        log.setBizType(bizType.trim());
        log.setActionType(actionType.trim());
        log.setActionName(trimToNull(actionName));
        log.setBeforeData(normalizeJsonPayload(beforeData));
        log.setAfterData(normalizeJsonPayload(afterData));
        log.setSuccessFlag(normalizeSuccessFlag(successFlag));
        log.setErrorMessage(trimToNull(errorMessage));
        log.setRemark(trimToNull(remark));
        log.setClientIp(resolveClientIp());
        log.setTraceId(MDC.get("traceId"));
        log.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    @Override
    public List<AuditLogVO> listTargetLogs(String targetType, Long targetId, int limit) {
        return listTargetLogs(targetType, targetId, List.of(), limit);
    }

    @Override
    public List<AuditLogVO> listTargetLogs(String targetType, Long targetId, List<String> actionTypes, int limit) {
        if (!StringUtils.hasText(targetType) || targetId == null) {
            return List.of();
        }
        int size = normalizeLimit(limit);
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<AuditLog>()
            .eq(AuditLog::getTargetType, targetType.trim())
            .eq(AuditLog::getTargetId, targetId)
            .orderByDesc(AuditLog::getId)
            .last("LIMIT " + size);
        List<String> normalizedActionTypes = normalizeActionTypes(actionTypes);
        if (!normalizedActionTypes.isEmpty()) {
            wrapper.in(AuditLog::getActionType, normalizedActionTypes);
        }
        List<AuditLog> logs = auditLogMapper.selectList(wrapper);
        return logs.stream().map(this::toVO).toList();
    }

    @Override
    public List<AuditLogVO> listRecentLogs(String bizType, int limit) {
        int size = normalizeLimit(limit);
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<AuditLog>()
            .orderByDesc(AuditLog::getId)
            .last("LIMIT " + size);
        if (StringUtils.hasText(bizType)) {
            wrapper.eq(AuditLog::getBizType, bizType.trim());
        }
        List<AuditLog> logs = auditLogMapper.selectList(wrapper);
        return logs.stream().map(this::toVO).toList();
    }

    private AuditLogVO toVO(AuditLog log) {
        AuditLogVO vo = new AuditLogVO();
        vo.setId(log.getId());
        vo.setBizType(log.getBizType());
        vo.setTargetType(log.getTargetType());
        vo.setActionType(log.getActionType());
        vo.setActionName(log.getActionName());
        vo.setOperatorName(StringUtils.hasText(log.getOperatorName()) ? log.getOperatorName() : SYSTEM_OPERATOR_NAME);
        vo.setTargetId(log.getTargetId());
        vo.setTargetUserId(log.getTargetUserId());
        vo.setTargetName(log.getTargetName());
        vo.setSummary(buildSummary(log));
        vo.setRemark(log.getRemark());
        vo.setBeforeData(log.getBeforeData());
        vo.setAfterData(log.getAfterData());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    private String buildSummary(AuditLog log) {
        if (StringUtils.hasText(log.getRemark())) {
            return log.getRemark().trim();
        }
        if (StringUtils.hasText(log.getActionName())) {
            return log.getActionName().trim();
        }
        if (StringUtils.hasText(log.getActionType())) {
            return log.getActionType().trim();
        }
        return "audit log";
    }

    private int normalizeLimit(int limit) {
        return limit <= 0 ? 10 : Math.min(limit, 50);
    }

    private int normalizeSuccessFlag(Integer successFlag) {
        return successFlag != null && successFlag == 0 ? 0 : 1;
    }

    private List<String> normalizeActionTypes(List<String> actionTypes) {
        if (actionTypes == null || actionTypes.isEmpty()) {
            return List.of();
        }
        return actionTypes.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .toList();
    }

    private String normalizeJsonPayload(String value) {
        return StringUtils.hasText(value) ? value.trim() : "{}";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String resolveClientIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        HttpServletRequest request = servletAttributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            int comma = forwarded.indexOf(',');
            return comma >= 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }
}
