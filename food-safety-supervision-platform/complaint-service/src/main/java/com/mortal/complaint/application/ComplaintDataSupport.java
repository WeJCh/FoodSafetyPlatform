package com.mortal.complaint.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.complaint.client.regulation.dto.EnterpriseKeyReasonUpsertDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortal.complaint.client.regulation.RegulationInternalClient;
import com.mortal.complaint.client.regulation.vo.InternalEnterpriseDetailVO;
import com.mortal.complaint.client.regulation.vo.InternalEnterpriseSummaryVO;
import com.mortal.complaint.client.regulation.vo.InternalRegulatorIdentityVO;
import com.mortal.complaint.client.regulation.vo.InternalRegulatorSummaryVO;
import com.mortal.platform.common.ApiResponse;
import com.mortal.complaint.domain.entity.Complaint;
import com.mortal.complaint.domain.entity.ComplaintHandle;
import com.mortal.complaint.domain.enums.ComplaintStatus;
import com.mortal.complaint.domain.service.ComplaintStatusFlow;
import com.mortal.complaint.infrastructure.mapper.ComplaintHandleMapper;
import com.mortal.complaint.infrastructure.mapper.ComplaintMapper;
import com.mortal.complaint.vo.ComplaintHandleVO;
import com.mortal.complaint.vo.ComplaintListVO;
import com.mortal.complaint.vo.ComplaintTrackVO;
import com.mortal.complaint.vo.ComplaintVO;
import com.mortal.complaint.vo.EnterpriseProfileVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 投诉数据支持。
 */
@Service
public class ComplaintDataSupport {

    public static final String ROLE_ADMIN = "REGULATOR_ADMIN";
    public static final String ROLE_ENFORCER = "REGULATOR_ENFORCER";
    private static final int MAX_IMAGE_COUNT = 5;

    private final ComplaintMapper complaintMapper;
    private final ComplaintHandleMapper complaintHandleMapper;
    private final RegulationInternalClient regulationInternalClient;
    private final ObjectMapper objectMapper;
    private final String regulationInternalToken;

    public ComplaintDataSupport(ComplaintMapper complaintMapper,
                                ComplaintHandleMapper complaintHandleMapper,
                                RegulationInternalClient regulationInternalClient,
                                ObjectMapper objectMapper,
                                @Value("${regulation.internal.token:regulation-internal-token}")
                                String regulationInternalToken) {
        this.complaintMapper = complaintMapper;
        this.complaintHandleMapper = complaintHandleMapper;
        this.regulationInternalClient = regulationInternalClient;
        this.objectMapper = objectMapper;
        this.regulationInternalToken = regulationInternalToken;
    }

    /**
     * 获取投诉。
     * @param id 投诉ID
     * @return 投诉
     */
    public Complaint requireComplaint(Long id) {
        Complaint complaint = complaintMapper.selectById(id);
        if (complaint == null || isDeleted(complaint.getDeleted())) {
            throw new IllegalArgumentException("complaint not found");
        }
        return complaint;
    }

    /**
     * 获取企业详情。
     * @param id 企业ID
     * @return 企业详情
     */
    public InternalEnterpriseDetailVO requireEnterprise(Long id) {
        ApiResponse<InternalEnterpriseDetailVO> response =
            regulationInternalClient.getEnterpriseById(id, regulationInternalToken);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new IllegalArgumentException("enterprise not found");
        }
        return response.getData();
    }

    /**
     * 统计企业已受理投诉数量。
     * @param enterpriseId 企业ID
     * @param since 统计时间
     * @return 已受理投诉数量
     */
    public long countAcceptedComplaints(Long enterpriseId, LocalDateTime since) {
        if (enterpriseId == null || since == null) {
            return 0L;
        }
        return complaintMapper.selectCount(new LambdaQueryWrapper<Complaint>()
            .eq(Complaint::getDeleted, 0)
            .eq(Complaint::getEnterpriseId, enterpriseId)
            .isNotNull(Complaint::getAcceptedTime)
            .ge(Complaint::getAcceptedTime, since)
            .ne(Complaint::getStatus, ComplaintStatus.REJECTED));
    }

    /**
     * 标记企业为关键企业。
     * @param enterpriseId 企业ID
     * @param reasonType 原因类型
     * @param reasonDetail 原因详情
     * @param sourceType 来源类型
     * @param sourceId 来源ID
     * @param operatorId 操作员ID
     */
    public void markEnterpriseAsKey(Long enterpriseId,
                                    String reasonType,
                                    String reasonDetail,
                                    String sourceType,
                                    Long sourceId,
                                    Long operatorId) {
        EnterpriseKeyReasonUpsertDTO dto = new EnterpriseKeyReasonUpsertDTO();
        dto.setReasonType(reasonType);
        dto.setReasonDetail(reasonDetail);
        dto.setSourceType(sourceType);
        dto.setSourceId(sourceId);
        dto.setOperatorId(operatorId);
        ApiResponse<Void> response =
            regulationInternalClient.markEnterpriseAsKey(enterpriseId, dto, regulationInternalToken);
        if (response == null || !response.isSuccess()) {
            throw new IllegalArgumentException("mark enterprise as key failed");
        }
    }

    /**
     * 获取监管员身份。
     * @param userId 用户ID
     * @return 监管员身份
     */
    public InternalRegulatorIdentityVO requireRegulatorByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("unauthorized");
        }
        ApiResponse<InternalRegulatorIdentityVO> response =
            regulationInternalClient.getRegulatorByUserId(userId, regulationInternalToken);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new IllegalArgumentException("regulator not found");
        }
        InternalRegulatorIdentityVO regulator = response.getData();
        if (regulator.getStatus() != null && regulator.getStatus() != 1) {
            throw new IllegalArgumentException("regulator disabled");
        }
        return regulator;
    }
    /**
     * 获取监管员详情。
     * @param regulatorId 监管员ID
     * @param notFoundMessage 未找到消息
     * @return 监管员详情
     */
    public InternalRegulatorSummaryVO requireRegulatorById(Long regulatorId, String notFoundMessage) {
        ApiResponse<InternalRegulatorSummaryVO> response =
            regulationInternalClient.getRegulatorById(regulatorId, regulationInternalToken);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new IllegalArgumentException(notFoundMessage);
        }
        return response.getData();
    }

    /**
     * 验证监管员角色。
     * @param regulator 监管员
     * @param roleType 角色类型
     */
    public void requireRole(InternalRegulatorIdentityVO regulator, String roleType) {
        if (regulator == null || !roleType.equalsIgnoreCase(regulator.getRoleType())) {
            throw new IllegalArgumentException("invalid regulator role");
        }
    }

    /**
     * 检查投诉是否在监管员管辖范围内。
     * @param regulator 监管员
     * @param enterpriseId 企业ID
     * @return 是否在管辖范围内
     */
    public boolean isComplaintInRegion(InternalRegulatorIdentityVO regulator, Long enterpriseId) {
        if (regulator == null || enterpriseId == null) {
            return false;
        }
        return resolveEnterpriseIdsByRegion(regulator).contains(enterpriseId);
    }

    /**
     * 根据企业名称获取企业ID列表。
     * @param enterpriseName 企业名称
     * @return 企业ID列表
     */
    public List<Long> resolveEnterpriseIdsByName(String enterpriseName) {
        if (!StringUtils.hasText(enterpriseName)) {
            return null;
        }
        ApiResponse<List<Long>> response =
            regulationInternalClient.queryEnterpriseIdsByName(enterpriseName.trim(), regulationInternalToken);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return List.of();
        }
        return response.getData();
    }

    /**
     * 根据监管员名称获取监管员ID列表。
     * @param regulatorName 监管员名称
     * @return 监管员ID列表
     */
    public List<Long> resolveRegulatorIdsByName(String regulatorName) {
        if (!StringUtils.hasText(regulatorName)) {
            return null;
        }
        ApiResponse<List<Long>> response =
            regulationInternalClient.queryRegulatorIdsByName(regulatorName.trim(), regulationInternalToken);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return List.of();
        }
        return response.getData();
    }

    /**
     * 根据监管员获取企业ID列表。
     * @param regulator 监管员
     * @return 企业ID列表
     */
    public List<Long> resolveEnterpriseIdsByRegion(InternalRegulatorIdentityVO regulator) {
        if (regulator == null || regulator.getId() == null) {
            return List.of();
        }
        ApiResponse<List<Long>> response =
            regulationInternalClient.getScopeEnterpriseIds(regulator.getId(), regulationInternalToken);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return List.of();
        }
        return response.getData();
    }

    public ComplaintVO toVOWithNames(Complaint complaint) {
        Map<Long, String> enterpriseNames = loadEnterpriseNames(List.of(complaint));
        Map<Long, String> regulatorNames = loadRegulatorNames(List.of(complaint));
        Map<Long, String> handleResults = loadHandleResults(List.of(complaint));
        ComplaintVO vo = toVO(complaint, enterpriseNames, regulatorNames);
        vo.setHandleResult(handleResults.get(complaint.getId()));
        return vo;
    }

    public ComplaintListVO toListVO(Complaint complaint, Map<Long, String> enterpriseNames) {
        ComplaintListVO vo = new ComplaintListVO();
        vo.setId(complaint.getId());
        vo.setComplaintNo(complaint.getComplaintNo());
        vo.setEnterpriseId(complaint.getEnterpriseId());
        vo.setEnterpriseName(enterpriseNames.get(complaint.getEnterpriseId()));
        vo.setStatus(complaint.getStatus());
        vo.setCreateTime(complaint.getCreateTime());
        vo.setUpdateTime(complaint.getUpdateTime());
        return vo;
    }

    public ComplaintVO toVO(Complaint complaint,
                            Map<Long, String> enterpriseNames,
                            Map<Long, String> regulatorNames) {
        ComplaintVO vo = new ComplaintVO();
        vo.setId(complaint.getId());
        vo.setComplaintNo(complaint.getComplaintNo());
        vo.setEnterpriseId(complaint.getEnterpriseId());
        vo.setEnterpriseName(enterpriseNames.get(complaint.getEnterpriseId()));
        vo.setComplaintType(complaint.getComplaintType());
        vo.setContent(complaint.getContent());
        vo.setImageUrls(parseImageUrls(complaint.getImageUrls()));
        vo.setAcceptedBy(complaint.getAcceptedBy());
        vo.setAcceptedByName(regulatorNames.get(complaint.getAcceptedBy()));
        vo.setAcceptedTime(complaint.getAcceptedTime());
        vo.setStatus(complaint.getStatus());
        vo.setAssignedTo(complaint.getAssignedTo());
        vo.setAssignedToName(regulatorNames.get(complaint.getAssignedTo()));
        vo.setAssignedBy(complaint.getAssignedBy());
        vo.setAssignedByName(regulatorNames.get(complaint.getAssignedBy()));
        vo.setAssignedTime(complaint.getAssignedTime());
        vo.setDeadlineTime(complaint.getDeadlineTime());
        vo.setProcessedBy(complaint.getProcessedBy());
        vo.setProcessedByName(regulatorNames.get(complaint.getProcessedBy()));
        vo.setProcessedTime(complaint.getProcessedTime());
        vo.setFeedbackSummary(complaint.getFeedbackSummary());
        vo.setRejectedBy(complaint.getRejectedBy());
        vo.setRejectedByName(regulatorNames.get(complaint.getRejectedBy()));
        vo.setRejectedTime(complaint.getRejectedTime());
        vo.setRejectReason(complaint.getRejectReason());
        vo.setCreateTime(complaint.getCreateTime());
        vo.setUpdateTime(complaint.getUpdateTime());
        return vo;
    }

    public ComplaintTrackVO toTrackVO(Complaint complaint) {
        ComplaintTrackVO vo = new ComplaintTrackVO();
        vo.setComplaintNo(complaint.getComplaintNo());
        vo.setStatus(complaint.getStatus());
        vo.setUpdateTime(complaint.getUpdateTime());
        return vo;
    }

    /**
     * 加载处理结果。
     * @param complaints 投诉列表
     * @return 处理结果
     */
    public Map<Long, String> loadHandleResults(List<Complaint> complaints) {
        if (complaints == null || complaints.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> complaintIds = complaints.stream()
            .map(Complaint::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (complaintIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ComplaintHandle> handles = complaintHandleMapper.selectList(new LambdaQueryWrapper<ComplaintHandle>()
            .in(ComplaintHandle::getComplaintId, complaintIds)
            .eq(ComplaintHandle::getDeleted, 0)
            .orderByDesc(ComplaintHandle::getHandleTime));
        if (handles.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> result = new java.util.HashMap<>();
        for (ComplaintHandle handle : handles) {
            result.putIfAbsent(handle.getComplaintId(), handle.getHandleResult());
        }
        return result;
    }

    /**
     * 加载处理详情。
     * @param complaintId 投诉ID
     * @return 处理详情
     */
    public List<ComplaintHandleVO> loadHandleDetails(Long complaintId) {
        if (complaintId == null) {
            return List.of();
        }
        List<ComplaintHandle> handles = complaintHandleMapper.selectList(new LambdaQueryWrapper<ComplaintHandle>()
            .eq(ComplaintHandle::getComplaintId, complaintId)
            .eq(ComplaintHandle::getDeleted, 0)
            .orderByDesc(ComplaintHandle::getHandleTime));
        if (handles.isEmpty()) {
            return List.of();
        }
        Set<Long> handlerIds = handles.stream()
            .map(ComplaintHandle::getHandlerId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> handlerNames = loadRegulatorNamesByIds(handlerIds);
        return handles.stream().map(handle -> {
            ComplaintHandleVO vo = new ComplaintHandleVO();
            vo.setHandlerId(handle.getHandlerId());
            vo.setHandlerName(handlerNames.get(handle.getHandlerId()));
            vo.setHandleResult(handle.getHandleResult());
            vo.setHandleTime(handle.getHandleTime());
            return vo;
        }).toList();
    }

    /**
     * 加载企业名称。
     * @param complaints 投诉列表
     * @return 企业名称
     */
    public Map<Long, String> loadEnterpriseNames(List<Complaint> complaints) {
        if (complaints == null || complaints.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> enterpriseIds = complaints.stream()
            .map(Complaint::getEnterpriseId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (enterpriseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        ApiResponse<List<InternalEnterpriseSummaryVO>> response =
            regulationInternalClient.getEnterpriseSummaries(enterpriseIds.stream().toList(), regulationInternalToken);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return Collections.emptyMap();
        }
        return response.getData().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(InternalEnterpriseSummaryVO::getId,
                InternalEnterpriseSummaryVO::getEnterpriseName,
                (a, b) -> a));
    }

    /**
     * 加载监管员名称。
     * @param complaints 投诉列表
     * @return 监管员名称
     */
    public Map<Long, String> loadRegulatorNames(List<Complaint> complaints) {
        if (complaints == null || complaints.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> regulatorIds = new LinkedHashSet<>();
        for (Complaint complaint : complaints) {
            if (complaint.getAssignedTo() != null) {
                regulatorIds.add(complaint.getAssignedTo());
            }
            if (complaint.getAssignedBy() != null) {
                regulatorIds.add(complaint.getAssignedBy());
            }
            if (complaint.getAcceptedBy() != null) {
                regulatorIds.add(complaint.getAcceptedBy());
            }
            if (complaint.getProcessedBy() != null) {
                regulatorIds.add(complaint.getProcessedBy());
            }
            if (complaint.getRejectedBy() != null) {
                regulatorIds.add(complaint.getRejectedBy());
            }
        }
        return loadRegulatorNamesByIds(regulatorIds);
    }

    /**
     * 加载监管员名称。
     * @param regulatorIds 监管员ID列表
     * @return 监管员名称
     */
    public Map<Long, String> loadRegulatorNamesByIds(Set<Long> regulatorIds) {
        if (regulatorIds == null || regulatorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        ApiResponse<List<InternalRegulatorSummaryVO>> response =
            regulationInternalClient.getRegulatorSummaries(regulatorIds.stream().toList(), regulationInternalToken);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return Collections.emptyMap();
        }
        return response.getData().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(InternalRegulatorSummaryVO::getId, InternalRegulatorSummaryVO::getName, (a, b) -> a));
    }

    /**
     * 转换企业详情VO。
     * @param enterprise 企业详情
     * @return 企业详情VO
     */
    public EnterpriseProfileVO toEnterpriseProfileVO(InternalEnterpriseDetailVO enterprise) {
        EnterpriseProfileVO vo = new EnterpriseProfileVO();
        vo.setId(enterprise.getId());
        vo.setUserId(enterprise.getUserId());
        vo.setEnterpriseName(enterprise.getEnterpriseName());
        vo.setLicenseNo(enterprise.getLicenseNo());
        vo.setCreditCode(enterprise.getCreditCode());
        vo.setLegalRepresentative(enterprise.getLegalRepresentative());
        vo.setRegionId(enterprise.getRegionId());
        vo.setAddressId(enterprise.getAddressId());
        vo.setAddressDetail(enterprise.getAddressDetail());
        vo.setPrincipal(enterprise.getPrincipal());
        vo.setPrincipalPhone(enterprise.getPrincipalPhone());
        vo.setRegulatorName(enterprise.getRegulatorName());
        vo.setStatus(enterprise.getStatus());
        vo.setApprovalStatus(enterprise.getApprovalStatus());
        vo.setRegionPath(List.of());
        vo.setRegionPathText("");
        return vo;
    }

    /**
     * 保存处理结果。
     * @param complaintId 投诉ID
     * @param handlerId 处理员ID
     * @param result 处理结果
     */
    public void saveSingleHandle(Long complaintId, Long handlerId, String result) {
        LocalDateTime now = LocalDateTime.now();
        ComplaintHandle existing = complaintHandleMapper.selectOne(new LambdaQueryWrapper<ComplaintHandle>()
            .eq(ComplaintHandle::getComplaintId, complaintId)
            .eq(ComplaintHandle::getDeleted, 0)
            .last("limit 1"));
        if (existing == null) {
            ComplaintHandle handle = new ComplaintHandle();
            handle.setComplaintId(complaintId);
            handle.setHandlerId(handlerId);
            handle.setHandleResult(result);
            handle.setHandleTime(now);
            handle.setCreateTime(now);
            handle.setUpdateTime(now);
            handle.setDeleted(0);
            complaintHandleMapper.insert(handle);
            return;
        }
        existing.setHandlerId(handlerId);
        existing.setHandleResult(result);
        existing.setHandleTime(now);
        existing.setUpdateTime(now);
        complaintHandleMapper.updateById(existing);
    }

    /**
     * 转移投诉状态。
     * @param complaint 投诉
     * @param target 目标状态
     */
    public void transitionComplaint(Complaint complaint, ComplaintStatus target) {
        ComplaintStatusFlow.validateTransition(complaint.getStatus(), target);
        complaint.setStatus(target);
    }

    public String generateComplaintNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 9000) + 1000;
        return "CPT" + time + random;
    }
    /**
     * 序列化图片URL列表。
     * @param imageUrls 图片URL列表
     * @return 序列化后的图片URL列表
     */
    public String serializeImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return null;
        }
        List<String> normalized = imageUrls.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .limit(MAX_IMAGE_COUNT)
            .toList();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 解析图片URL列表。
     * @param value 图片URL列表
     * @return 解析后的图片URL列表
     */
    public List<String> parseImageUrls(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    public String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    public String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}




