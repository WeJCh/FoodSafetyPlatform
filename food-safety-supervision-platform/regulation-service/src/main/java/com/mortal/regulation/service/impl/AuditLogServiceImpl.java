package com.mortal.regulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortal.regulation.entity.AddrRegion;
import com.mortal.regulation.entity.AuditLog;
import com.mortal.regulation.entity.FoodEnterprise;
import com.mortal.regulation.entity.FoodRegulator;
import com.mortal.regulation.entity.PublicBulletin;
import com.mortal.regulation.mapper.AddrRegionMapper;
import com.mortal.regulation.mapper.AuditLogMapper;
import com.mortal.regulation.service.AuditLogService;
import com.mortal.regulation.vo.AuditLogVO;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
public class AuditLogServiceImpl implements AuditLogService {

    private static final String SERVICE_NAME = "regulation-service";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final AuditLogMapper auditLogMapper;
    private final AddrRegionMapper addrRegionMapper;
    private final ObjectMapper objectMapper;

    public AuditLogServiceImpl(AuditLogMapper auditLogMapper,
                               AddrRegionMapper addrRegionMapper,
                               ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.addrRegionMapper = addrRegionMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void recordRegulatorAudit(Long operatorUserId,
                                     String operatorName,
                                     String actionType,
                                     String actionName,
                                     FoodRegulator beforeRegulator,
                                     FoodRegulator afterRegulator,
                                     List<Long> beforeRegionIds,
                                     List<Long> afterRegionIds,
                                     String remark) {
        FoodRegulator regulator = afterRegulator != null ? afterRegulator : beforeRegulator;
        if (regulator == null) {
            return;
        }
        AuditLog log = new AuditLog();
        log.setServiceName(SERVICE_NAME);
        log.setOperatorUserId(operatorUserId);
        log.setOperatorUserType("ADMIN");
        log.setOperatorName(StringUtils.hasText(operatorName) ? operatorName : buildOperatorFallback(operatorUserId));
        log.setTargetType("REGULATOR");
        log.setTargetId(regulator.getId());
        log.setTargetUserId(regulator.getUserId());
        log.setTargetName(regulator.getName());
        log.setBizType("REGULATOR");
        log.setActionType(actionType);
        log.setActionName(actionName);
        log.setBeforeData(writeRegulatorSnapshot(beforeRegulator, beforeRegionIds));
        log.setAfterData(writeRegulatorSnapshot(afterRegulator, afterRegionIds));
        log.setSuccessFlag(1);
        log.setRemark(normalizeRemark(remark));
        log.setClientIp(resolveClientIp());
        log.setTraceId(MDC.get("traceId"));
        log.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    @Override
    public void recordEnterpriseAudit(Long operatorUserId,
                                      String operatorUserType,
                                      String operatorName,
                                      String actionType,
                                      String actionName,
                                      FoodEnterprise beforeEnterprise,
                                      FoodEnterprise afterEnterprise,
                                      String remark) {
        FoodEnterprise enterprise = afterEnterprise != null ? afterEnterprise : beforeEnterprise;
        if (enterprise == null) {
            return;
        }
        AuditLog log = new AuditLog();
        log.setServiceName(SERVICE_NAME);
        log.setOperatorUserId(operatorUserId);
        log.setOperatorUserType(normalizeText(operatorUserType));
        log.setOperatorName(StringUtils.hasText(operatorName) ? operatorName : buildOperatorFallback(operatorUserId));
        log.setTargetType("ENTERPRISE");
        log.setTargetId(enterprise.getId());
        log.setTargetUserId(enterprise.getUserId());
        log.setTargetName(enterprise.getEnterpriseName());
        log.setBizType("ENTERPRISE");
        log.setActionType(actionType);
        log.setActionName(actionName);
        log.setBeforeData(writeEnterpriseSnapshot(beforeEnterprise));
        log.setAfterData(writeEnterpriseSnapshot(afterEnterprise));
        log.setSuccessFlag(1);
        log.setRemark(normalizeRemark(remark));
        log.setClientIp(resolveClientIp());
        log.setTraceId(MDC.get("traceId"));
        log.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    @Override
    public void recordBulletinAudit(Long operatorUserId,
                                    String operatorUserType,
                                    String operatorName,
                                    String actionType,
                                    String actionName,
                                    PublicBulletin beforeBulletin,
                                    PublicBulletin afterBulletin,
                                    String remark) {
        PublicBulletin bulletin = afterBulletin != null ? afterBulletin : beforeBulletin;
        if (bulletin == null) {
            return;
        }
        AuditLog log = new AuditLog();
        log.setServiceName(SERVICE_NAME);
        log.setOperatorUserId(operatorUserId);
        log.setOperatorUserType(normalizeText(operatorUserType));
        log.setOperatorName(StringUtils.hasText(operatorName) ? operatorName : buildOperatorFallback(operatorUserId));
        log.setTargetType("BULLETIN");
        log.setTargetId(bulletin.getId());
        log.setTargetName(bulletin.getTitle());
        log.setBizType("BULLETIN");
        log.setActionType(actionType);
        log.setActionName(actionName);
        log.setBeforeData(writeBulletinSnapshot(beforeBulletin));
        log.setAfterData(writeBulletinSnapshot(afterBulletin));
        log.setSuccessFlag(1);
        log.setRemark(normalizeRemark(remark));
        log.setClientIp(resolveClientIp());
        log.setTraceId(MDC.get("traceId"));
        log.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    @Override
    public List<AuditLogVO> listEnterpriseLogs(Long enterpriseId, int limit) {
        int size = normalizeLimit(limit);
        List<AuditLog> logs = auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
            .eq(AuditLog::getTargetType, "ENTERPRISE")
            .eq(AuditLog::getTargetId, enterpriseId)
            .orderByDesc(AuditLog::getId)
            .last("LIMIT " + size));
        return logs.stream().map(this::toVO).toList();
    }

    @Override
    public List<AuditLogVO> listRecentEnterpriseLogs(int limit) {
        int size = normalizeLimit(limit);
        List<AuditLog> logs = auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
            .eq(AuditLog::getTargetType, "ENTERPRISE")
            .orderByDesc(AuditLog::getId)
            .last("LIMIT " + size));
        return logs.stream().map(this::toVO).toList();
    }

    @Override
    public List<AuditLogVO> listBulletinLogs(Long bulletinId, int limit) {
        int size = normalizeLimit(limit);
        List<AuditLog> logs = auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
            .eq(AuditLog::getTargetType, "BULLETIN")
            .eq(AuditLog::getTargetId, bulletinId)
            .orderByDesc(AuditLog::getId)
            .last("LIMIT " + size));
        return logs.stream().map(this::toVO).toList();
    }

    @Override
    public List<AuditLogVO> listRecentBulletinLogs(int limit) {
        int size = normalizeLimit(limit);
        List<AuditLog> logs = auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
            .eq(AuditLog::getTargetType, "BULLETIN")
            .orderByDesc(AuditLog::getId)
            .last("LIMIT " + size));
        return logs.stream().map(this::toVO).toList();
    }

    @Override
    public List<AuditLogVO> listRegulatorLogs(Long regulatorId, int limit) {
        int size = normalizeLimit(limit);
        List<AuditLog> logs = auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
            .eq(AuditLog::getTargetType, "REGULATOR")
            .eq(AuditLog::getTargetId, regulatorId)
            .orderByDesc(AuditLog::getId)
            .last("LIMIT " + size));
        return logs.stream().map(this::toVO).toList();
    }

    @Override
    public List<AuditLogVO> listRecentRegulatorLogs(int limit) {
        int size = normalizeLimit(limit);
        List<AuditLog> logs = auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
            .eq(AuditLog::getTargetType, "REGULATOR")
            .orderByDesc(AuditLog::getId)
            .last("LIMIT " + size));
        return logs.stream().map(this::toVO).toList();
    }

    private int normalizeLimit(int limit) {
        return limit <= 0 ? 10 : Math.min(limit, 50);
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeRemark(String remark) {
        return StringUtils.hasText(remark) ? remark.trim() : null;
    }

    private AuditLogVO toVO(AuditLog log) {
        AuditLogVO vo = new AuditLogVO();
        vo.setId(log.getId());
        vo.setActionType(log.getActionType());
        vo.setActionName(log.getActionName());
        vo.setOperatorName(StringUtils.hasText(log.getOperatorName())
            ? log.getOperatorName()
            : buildOperatorFallback(log.getOperatorUserId()));
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
        Map<String, Object> before = readJsonMap(log.getBeforeData());
        Map<String, Object> after = readJsonMap(log.getAfterData());
        String actionType = String.valueOf(log.getActionType());

        if ("ENTERPRISE_SUBMIT".equals(actionType)) {
            return "\u4f01\u4e1a\u9996\u6b21\u63d0\u4ea4\u5907\u6848\u6863\u6848\uff0c\u5f53\u524d\u5ba1\u6838\u72b6\u6001\u4e3a"
                + approvalText(after.get("approvalStatus"));
        }
        if ("ENTERPRISE_UPDATE".equals(actionType)) {
            return "\u4f01\u4e1a\u66f4\u65b0\u5907\u6848\u6863\u6848\uff0c\u5ba1\u6838\u72b6\u6001\u5df2\u91cd\u7f6e\u4e3a"
                + approvalText(after.get("approvalStatus"));
        }
        if ("ENTERPRISE_APPROVE".equals(actionType) || "ENTERPRISE_REJECT".equals(actionType)) {
            return "\u4f01\u4e1a\u5ba1\u6838\u72b6\u6001\u7531" + approvalText(before.get("approvalStatus"))
                + "\u8c03\u6574\u4e3a" + approvalText(after.get("approvalStatus"));
        }
        if ("ENTERPRISE_DELETE".equals(actionType)) {
            return "\u5220\u9664\u4f01\u4e1a\u6863\u6848\uff1a" + nameText(after, before);
        }
        if ("BULLETIN_CREATE".equals(actionType)) {
            return "\u521b\u5efa\u516c\u544a\u8349\u7a3f\uff0c\u5f53\u524d\u72b6\u6001\u4e3a"
                + bulletinStatusText(after.get("status"));
        }
        if ("BULLETIN_UPDATE".equals(actionType)) {
            return "\u66f4\u65b0\u516c\u544a\u5185\u5bb9\uff0c\u5f53\u524d\u72b6\u6001\u4e3a"
                + bulletinStatusText(after.get("status"));
        }
        if ("BULLETIN_PUBLISH".equals(actionType) || "BULLETIN_OFFLINE".equals(actionType)) {
            return "\u516c\u544a\u72b6\u6001\u7531" + bulletinStatusText(before.get("status"))
                + "\u8c03\u6574\u4e3a" + bulletinStatusText(after.get("status"));
        }

        String beforeRegion = regionText(before);
        String afterRegion = regionText(after);
        return switch (actionType) {
            case "REGULATOR_CREATE" -> "\u521b\u5efa\u76d1\u7ba1\u4eba\u5458\u6863\u6848\uff0c\u521d\u59cb\u8f96\u533a\u4e3a\uff1a"
                + afterRegion;
            case "REGULATOR_UPDATE" -> "\u66f4\u65b0\u76d1\u7ba1\u4eba\u5458\u6863\u6848\uff0c\u5f53\u524d\u8f96\u533a\u4e3a\uff1a"
                + afterRegion;
            case "REGULATOR_STATUS_CHANGE" -> "\u8d26\u53f7\u72b6\u6001\u7531"
                + statusText(before.get("status")) + "\u8c03\u6574\u4e3a" + statusText(after.get("status"));
            case "REGULATOR_REGION_ADJUST" -> "\u8f96\u533a\u7531" + beforeRegion + "\u8c03\u6574\u4e3a" + afterRegion;
            case "REGULATOR_DELETE" -> "\u5220\u9664\u76d1\u7ba1\u4eba\u5458\u6863\u6848\u5e76\u56de\u6536\u8f96\u533a\u6743\u9650";
            default -> StringUtils.hasText(log.getActionName())
                ? log.getActionName()
                : "\u5ba1\u8ba1\u4e8b\u4ef6";
        };
    }

    private String statusText(Object value) {
        return "1".equals(String.valueOf(value)) ? "\u542f\u7528" : "\u505c\u7528";
    }

    private String approvalText(Object value) {
        return switch (String.valueOf(value)) {
            case "APPROVED" -> "\u5df2\u901a\u8fc7";
            case "REJECTED" -> "\u5df2\u9a73\u56de";
            case "PENDING" -> "\u5f85\u5ba1\u6838";
            default -> "-";
        };
    }

    private String bulletinStatusText(Object value) {
        return switch (String.valueOf(value)) {
            case "PUBLISHED" -> "\u5df2\u53d1\u5e03";
            case "OFFLINE" -> "\u5df2\u4e0b\u7ebf";
            case "DRAFT" -> "\u8349\u7a3f";
            default -> "-";
        };
    }

    private String nameText(Map<String, Object> after, Map<String, Object> before) {
        Object value = after.get("enterpriseName");
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            value = before.get("enterpriseName");
        }
        return value == null ? "-" : String.valueOf(value);
    }

    private String regionText(Map<String, Object> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return "-";
        }
        Object value = snapshot.get("regionPathText");
        if (value != null && StringUtils.hasText(String.valueOf(value))) {
            return String.valueOf(value);
        }
        Object ids = snapshot.get("regionIds");
        return ids == null ? "-" : String.valueOf(ids);
    }

    private String writeRegulatorSnapshot(FoodRegulator regulator, List<Long> regionIds) {
        if (regulator == null) {
            return "{}";
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("regulatorId", regulator.getId());
        snapshot.put("userId", regulator.getUserId());
        snapshot.put("name", regulator.getName());
        snapshot.put("phone", regulator.getPhone());
        snapshot.put("roleType", regulator.getRoleType());
        snapshot.put("status", regulator.getStatus());
        List<Long> safeRegionIds = regionIds == null ? List.of() : regionIds.stream().distinct().toList();
        snapshot.put("regionIds", safeRegionIds);
        snapshot.put("regionPathText", buildRegionPathText(safeRegionIds));
        return writeJson(snapshot, "failed to serialize regulator audit snapshot");
    }

    private String writeEnterpriseSnapshot(FoodEnterprise enterprise) {
        if (enterprise == null) {
            return "{}";
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("enterpriseId", enterprise.getId());
        snapshot.put("userId", enterprise.getUserId());
        snapshot.put("enterpriseName", enterprise.getEnterpriseName());
        snapshot.put("licenseNo", enterprise.getLicenseNo());
        snapshot.put("creditCode", enterprise.getCreditCode());
        snapshot.put("legalRepresentative", enterprise.getLegalRepresentative());
        snapshot.put("regionId", enterprise.getRegionId());
        snapshot.put("regionPathText", resolveRegionPath(enterprise.getRegionId()));
        snapshot.put("principal", enterprise.getPrincipal());
        snapshot.put("principalPhone", enterprise.getPrincipalPhone());
        snapshot.put("regulatorName", enterprise.getRegulatorName());
        snapshot.put("status", enterprise.getStatus());
        snapshot.put("approvalStatus", enterprise.getApprovalStatus());
        snapshot.put("approvalComment", enterprise.getApprovalComment());
        snapshot.put("approvedBy", enterprise.getApprovedBy());
        snapshot.put("approvedTime", enterprise.getApprovedTime());
        snapshot.put("deleted", enterprise.getDeleted());
        return writeJson(snapshot, "failed to serialize enterprise audit snapshot");
    }

    private String writeBulletinSnapshot(PublicBulletin bulletin) {
        if (bulletin == null) {
            return "{}";
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("bulletinId", bulletin.getId());
        snapshot.put("title", bulletin.getTitle());
        snapshot.put("category", bulletin.getCategory());
        snapshot.put("status", bulletin.getStatus());
        snapshot.put("createdBy", bulletin.getCreatedBy());
        snapshot.put("publishedBy", bulletin.getPublishedBy());
        snapshot.put("publishedTime", bulletin.getPublishedTime());
        snapshot.put("contentPreview", buildBulletinContentPreview(bulletin.getContent()));
        snapshot.put("contentLength", bulletin.getContent() == null ? 0 : bulletin.getContent().trim().length());
        return writeJson(snapshot, "failed to serialize bulletin audit snapshot");
    }

    private String buildBulletinContentPreview(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120);
    }

    private String writeJson(Map<String, Object> snapshot, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(errorMessage, ex);
        }
    }

    private String buildRegionPathText(List<Long> regionIds) {
        if (regionIds == null || regionIds.isEmpty()) {
            return "";
        }
        List<String> texts = new ArrayList<>();
        for (Long regionId : regionIds) {
            texts.add(resolveRegionPath(regionId));
        }
        return String.join(" ; ", texts);
    }

    private String resolveRegionPath(Long regionId) {
        if (regionId == null) {
            return "-";
        }
        List<String> path = new ArrayList<>();
        Long current = regionId;
        while (current != null) {
            AddrRegion region = addrRegionMapper.selectById(current);
            if (region == null || isDeleted(region.getDeleted())) {
                break;
            }
            path.add(0, region.getName());
            current = region.getParentId();
        }
        return path.isEmpty() ? "\u8f96\u533aID=" + regionId : String.join(" / ", path);
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

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
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
        return operatorUserId == null
            ? "\u7cfb\u7edf\u7ba1\u7406\u5458"
            : "\u7cfb\u7edf\u7ba1\u7406\u5458#" + operatorUserId;
    }
}
