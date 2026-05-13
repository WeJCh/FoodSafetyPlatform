package com.mortal.complaint.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortal.complaint.domain.entity.AuditLog;
import com.mortal.complaint.domain.entity.Complaint;
import com.mortal.complaint.infrastructure.mapper.AuditLogMapper;
import com.mortal.complaint.vo.AuditLogVO;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditLogService {

    private static final String SERVICE_NAME = "complaint-service";
    private static final String TARGET_TYPE_COMPLAINT = "COMPLAINT";
    private static final String BIZ_TYPE_COMPLAINT = "COMPLAINT";
    private static final String SYSTEM_OPERATOR_NAME = "系统";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    public void recordComplaintAudit(Long operatorUserId,
                                     String operatorUserType,
                                     String operatorName,
                                     String actionType,
                                     String actionName,
                                     Complaint beforeComplaint,
                                     Complaint afterComplaint,
                                     String remark) {
        Complaint complaint = afterComplaint != null ? afterComplaint : beforeComplaint;
        if (complaint == null) {
            return;
        }
        AuditLog log = new AuditLog();
        log.setServiceName(SERVICE_NAME);
        log.setOperatorUserId(operatorUserId);
        log.setOperatorUserType(trimToNull(operatorUserType));
        log.setOperatorName(StringUtils.hasText(operatorName) ? operatorName.trim() : buildOperatorFallback());
        log.setTargetType(TARGET_TYPE_COMPLAINT);
        log.setTargetId(complaint.getId());
        log.setTargetUserId(complaint.getSubmitterUserId());
        log.setTargetName(complaint.getComplaintNo());
        log.setBizType(BIZ_TYPE_COMPLAINT);
        log.setActionType(actionType);
        log.setActionName(actionName);
        log.setBeforeData(writeComplaintSnapshot(beforeComplaint));
        log.setAfterData(writeComplaintSnapshot(afterComplaint));
        log.setSuccessFlag(1);
        log.setRemark(trimToNull(remark));
        log.setClientIp(resolveClientIp());
        log.setTraceId(MDC.get("traceId"));
        log.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    public List<AuditLogVO> listComplaintLogs(Long complaintId, int limit) {
        int size = normalizeLimit(limit);
        List<AuditLog> logs = auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
            .eq(AuditLog::getTargetType, TARGET_TYPE_COMPLAINT)
            .eq(AuditLog::getTargetId, complaintId)
            .orderByDesc(AuditLog::getId)
            .last("LIMIT " + size));
        return logs.stream().map(this::toVO).toList();
    }

    public List<AuditLogVO> listRecentComplaintLogs(int limit) {
        int size = normalizeLimit(limit);
        List<AuditLog> logs = auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
            .eq(AuditLog::getTargetType, TARGET_TYPE_COMPLAINT)
            .orderByDesc(AuditLog::getId)
            .last("LIMIT " + size));
        return logs.stream().map(this::toVO).toList();
    }

    public List<AuditLogVO> listRecentComplaintLogs(List<Long> complaintIds, int limit) {
        if (complaintIds == null || complaintIds.isEmpty()) {
            return List.of();
        }
        int size = normalizeLimit(limit);
        List<AuditLog> logs = auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
            .eq(AuditLog::getTargetType, TARGET_TYPE_COMPLAINT)
            .in(AuditLog::getTargetId, complaintIds)
            .orderByDesc(AuditLog::getId)
            .last("LIMIT " + size));
        return logs.stream().map(this::toVO).toList();
    }

    private AuditLogVO toVO(AuditLog log) {
        AuditLogVO vo = new AuditLogVO();
        vo.setId(log.getId());
        vo.setActionType(log.getActionType());
        vo.setActionName(log.getActionName());
        vo.setOperatorName(StringUtils.hasText(log.getOperatorName()) ? log.getOperatorName() : buildOperatorFallback());
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
        Map<String, Object> before = readJsonMap(log.getBeforeData());
        Map<String, Object> after = readJsonMap(log.getAfterData());
        String actionType = String.valueOf(log.getActionType());
        switch (actionType) {
            case "COMPLAINT_SUBMIT":
                return "提交投诉，当前状态为" + statusText(after.get("status"));
            case "COMPLAINT_ACCEPT":
            case "COMPLAINT_ASSIGN":
            case "COMPLAINT_REASSIGN":
            case "COMPLAINT_PROCESS_START":
            case "COMPLAINT_HANDLE":
            case "COMPLAINT_REJECT":
                return "投诉状态由" + statusText(before.get("status")) + "调整为" + statusText(after.get("status"));
            default:
                break;
        }
        if (StringUtils.hasText(log.getActionName())) {
            return log.getActionName().trim();
        }
        if (StringUtils.hasText(log.getActionType())) {
            return log.getActionType().trim();
        }
        return "投诉日志";
    }

    private String statusText(Object value) {
        if (value == null) {
            return "-";
        }
        return switch (String.valueOf(value)) {
            case "SUBMITTED" -> "待受理";
            case "PENDING" -> "待分派";
            case "ASSIGNED" -> "已分派";
            case "PROCESSING" -> "处理中";
            case "FEEDBACKED" -> "已反馈";
            case "REJECTED" -> "已驳回";
            default -> String.valueOf(value);
        };
    }

    private String writeComplaintSnapshot(Complaint complaint) {
        if (complaint == null) {
            return "{}";
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("complaintId", complaint.getId());
        snapshot.put("complaintNo", complaint.getComplaintNo());
        snapshot.put("submitterUserId", complaint.getSubmitterUserId());
        snapshot.put("enterpriseId", complaint.getEnterpriseId());
        snapshot.put("complaintType", complaint.getComplaintType());
        snapshot.put("status", complaint.getStatus());
        snapshot.put("sourceType", complaint.getSourceType());
        snapshot.put("sourceId", complaint.getSourceId());
        snapshot.put("assignedTo", complaint.getAssignedTo());
        snapshot.put("assignedBy", complaint.getAssignedBy());
        snapshot.put("assignedTime", complaint.getAssignedTime());
        snapshot.put("deadlineTime", complaint.getDeadlineTime());
        snapshot.put("acceptedBy", complaint.getAcceptedBy());
        snapshot.put("acceptedTime", complaint.getAcceptedTime());
        snapshot.put("processedBy", complaint.getProcessedBy());
        snapshot.put("processedTime", complaint.getProcessedTime());
        snapshot.put("feedbackSummary", complaint.getFeedbackSummary());
        snapshot.put("rejectedBy", complaint.getRejectedBy());
        snapshot.put("rejectedTime", complaint.getRejectedTime());
        snapshot.put("rejectReason", complaint.getRejectReason());
        snapshot.put("deleted", complaint.getDeleted());
        return writeJson(snapshot);
    }

    private String writeJson(Map<String, Object> snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize complaint audit snapshot", ex);
        }
    }

    private Map<String, Object> readJsonMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private int normalizeLimit(int limit) {
        return limit <= 0 ? 10 : Math.min(limit, 50);
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

    private String buildOperatorFallback() {
        return SYSTEM_OPERATOR_NAME;
    }
}
