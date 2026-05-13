package com.mortal.complaint.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.complaint.client.regulation.RegulationInternalClient;
import com.mortal.complaint.domain.entity.Complaint;
import com.mortal.complaint.domain.enums.ComplaintStatus;
import com.mortal.complaint.dto.InternalStatsQueryDTO;
import com.mortal.complaint.infrastructure.mapper.ComplaintMapper;
import com.mortal.complaint.vo.InternalComplaintStatsOverviewVO;
import com.mortal.platform.common.ApiResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 投诉统计服务。
 */
@Service
public class ComplaintStatsService {

    private final ComplaintMapper complaintMapper;
    private final RegulationInternalClient regulationInternalClient;
    private final String regulationInternalToken;

    public ComplaintStatsService(ComplaintMapper complaintMapper,
                                 RegulationInternalClient regulationInternalClient,
                                 @Value("${regulation.internal.token:regulation-internal-token}")
                                 String regulationInternalToken) {
        this.complaintMapper = complaintMapper;
        this.regulationInternalClient = regulationInternalClient;
        this.regulationInternalToken = regulationInternalToken;
    }
    /**
     * 获取投诉统计概览。
     * 
     * @param queryDTO 查询条件
     * @return 投诉统计概览
     */
    public InternalComplaintStatsOverviewVO getOverview(InternalStatsQueryDTO queryDTO) {
        List<Long> enterpriseIds = resolveEnterpriseIds(queryDTO);
        InternalComplaintStatsOverviewVO overview = new InternalComplaintStatsOverviewVO();
        long totalCount = countByStatus(queryDTO, enterpriseIds, null);
        long submittedCount = countByStatus(queryDTO, enterpriseIds, ComplaintStatus.SUBMITTED);
        long pendingCount = countByStatus(queryDTO, enterpriseIds, ComplaintStatus.PENDING);
        long assignedCount = countByStatus(queryDTO, enterpriseIds, ComplaintStatus.ASSIGNED);
        long processingCount = countByStatus(queryDTO, enterpriseIds, ComplaintStatus.PROCESSING);
        long feedbackedCount = countByStatus(queryDTO, enterpriseIds, ComplaintStatus.FEEDBACKED);
        long rejectedCount = countByStatus(queryDTO, enterpriseIds, ComplaintStatus.REJECTED);
        overview.setTotalCount(totalCount);
        overview.setSubmittedCount(submittedCount);
        overview.setPendingCount(pendingCount);
        overview.setAssignedCount(assignedCount);
        overview.setProcessingCount(processingCount);
        overview.setFeedbackedCount(feedbackedCount);
        overview.setRejectedCount(rejectedCount);
        overview.setDoneCount(feedbackedCount + rejectedCount);
        overview.setTodoCount(submittedCount + pendingCount + assignedCount + processingCount);
        overview.setOverdueCount(countOverdue(queryDTO, enterpriseIds));
        return overview;
    }

    /**
     * 统计投诉数量。
     * 
     * @param queryDTO 查询条件
     * @param enterpriseIds 企业ID列表
     * @param status 状态
     * @return 投诉数量
     */
    private long countByStatus(InternalStatsQueryDTO queryDTO,
                               List<Long> enterpriseIds,
                               ComplaintStatus status) {
        LambdaQueryWrapper<Complaint> wrapper = buildBaseWrapper(queryDTO, enterpriseIds);
        if (wrapper == null) {
            return 0L;
        }
        if (status != null) {
            wrapper.eq(Complaint::getStatus, status);
        }
        return complaintMapper.selectCount(wrapper);
    }

    /**
     * 统计逾期投诉数量。
     * 
     * @param queryDTO 查询条件
     * @param enterpriseIds 企业ID列表
     * @return 逾期投诉数量
     */
    private long countOverdue(InternalStatsQueryDTO queryDTO, List<Long> enterpriseIds) {
        LambdaQueryWrapper<Complaint> wrapper = buildBaseWrapper(queryDTO, enterpriseIds);
        if (wrapper == null) {
            return 0L;
        }
        wrapper.isNotNull(Complaint::getDeadlineTime)
            .lt(Complaint::getDeadlineTime, LocalDateTime.now())
            .in(Complaint::getStatus, List.of(ComplaintStatus.ASSIGNED, ComplaintStatus.PROCESSING));
        return complaintMapper.selectCount(wrapper);
    }

    private LambdaQueryWrapper<Complaint> buildBaseWrapper(InternalStatsQueryDTO queryDTO, List<Long> enterpriseIds) {
        LambdaQueryWrapper<Complaint> wrapper = new LambdaQueryWrapper<Complaint>()
            .eq(Complaint::getDeleted, 0);
        if (queryDTO != null && queryDTO.getOwnerRegulatorId() != null) {
            wrapper.eq(Complaint::getAssignedTo, queryDTO.getOwnerRegulatorId());
            return wrapper;
        }
        if (enterpriseIds != null) {
            if (enterpriseIds.isEmpty()) {
                return null;
            }
            wrapper.in(Complaint::getEnterpriseId, enterpriseIds);
        }
        return wrapper;
    }
    /**
     * 解析企业ID列表。
     * 
     * @param queryDTO 查询条件
     * @return 企业ID列表
     */
    private List<Long> resolveEnterpriseIds(InternalStatsQueryDTO queryDTO) {
        if (queryDTO == null || queryDTO.getOwnerRegulatorId() != null) {
            return null;
        }
        if (queryDTO.getScopeRegulatorId() != null) {
            ApiResponse<List<Long>> response =
                regulationInternalClient.getScopeEnterpriseIds(queryDTO.getScopeRegulatorId(), regulationInternalToken);
            if (response == null || response.getCode() != 0 || response.getData() == null) {
                return List.of();
            }
            return response.getData();
        }
        if (queryDTO.getRegionId() == null && !StringUtils.hasText(queryDTO.getRegionIds())) {
            return null;
        }
        ApiResponse<List<Long>> response = regulationInternalClient.getEnterpriseIdsByScope(
            queryDTO.getRegionId(),
            queryDTO.getRegionIds(),
            regulationInternalToken
        );
        if (response == null || response.getCode() != 0 || response.getData() == null) {
            return List.of();
        }
        return response.getData();
    }
}
