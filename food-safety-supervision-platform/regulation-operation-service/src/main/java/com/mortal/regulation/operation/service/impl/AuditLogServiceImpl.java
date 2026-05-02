package com.mortal.regulation.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.regulation.operation.entity.AuditLog;
import com.mortal.regulation.operation.mapper.AuditLogMapper;
import com.mortal.regulation.operation.service.AuditLogService;
import com.mortal.regulation.operation.vo.AuditLogVO;
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

    private static final String SERVICE_NAME = "regulation-operation-service";

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
        if (!StringUtils.hasText(targetType) || targetId == null || !StringUtils.hasText(bizType)
            || !StringUtils.hasText(actionType)) {
            return;
        }
        AuditLog log = new AuditLog();
        log.setServiceName(SERVICE_NAME);
        log.setOperatorUserId(operatorUserId);
        log.setOperatorUserType(trimToNull(operatorUserType));
        log.setOperatorName(StringUtils.hasText(operatorName) ? operatorName.trim() : buildOperatorFallback(operatorUserId));
        log.setTargetType(targetType.trim());
        log.setTargetId(targetId);
        log.setTargetUserId(targetUserId);
        log.setTargetName(trimToNull(targetName));
        log.setBizType(bizType.trim());
        log.setActionType(actionType.trim());
        log.setActionName(trimToNull(actionName));
        log.setBeforeData(normalizeJsonPayload(beforeData));
        log.setAfterData(normalizeJsonPayload(afterData));
        log.setSuccessFlag(1);
        log.setRemark(trimToNull(remark));
        log.setClientIp(resolveClientIp());
        log.setTraceId(MDC.get("traceId"));
        log.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    @Override
    public List<AuditLogVO> listTargetLogs(String targetType, Long targetId, int limit) {
        if (!StringUtils.hasText(targetType) || targetId == null) {
            return List.of();
        }
        int size = normalizeLimit(limit);
        List<AuditLog> logs = auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
            .eq(AuditLog::getTargetType, targetType.trim())
            .eq(AuditLog::getTargetId, targetId)
            .orderByDesc(AuditLog::getId)
            .last("LIMIT " + size));
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
        vo.setOperatorName(StringUtils.hasText(log.getOperatorName()) ? log.getOperatorName() : buildOperatorFallback(log.getOperatorUserId()));
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

    private String buildOperatorFallback(Long operatorUserId) {
        return operatorUserId == null ? "system" : "user#" + operatorUserId;
    }
}
