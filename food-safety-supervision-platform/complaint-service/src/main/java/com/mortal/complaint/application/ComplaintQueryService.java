package com.mortal.complaint.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mortal.complaint.client.regulation.vo.InternalEnterpriseDetailVO;
import com.mortal.complaint.client.regulation.vo.InternalRegulatorIdentityVO;
import com.mortal.complaint.domain.enums.ComplaintStatus;
import com.mortal.complaint.vo.AuditLogVO;
import com.mortal.complaint.vo.ComplaintDetailVO;
import com.mortal.complaint.vo.ComplaintListVO;
import com.mortal.complaint.vo.ComplaintPublicStatsVO;
import com.mortal.complaint.vo.ComplaintVO;
import com.mortal.platform.common.PageResult;
import com.mortal.complaint.domain.entity.Complaint;
import com.mortal.complaint.infrastructure.mapper.ComplaintMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 投诉查询服务
 */
@Service
public class ComplaintQueryService {

    private final ComplaintMapper complaintMapper;
    private final AuditLogService auditLogService;
    private final ComplaintDataSupport complaintDataSupport;

    /**
     * 构造函数
     * @param complaintMapper 投诉Mapper
     * @param complaintDataSupport 投诉数据支持
     */
    public ComplaintQueryService(ComplaintMapper complaintMapper,
                                 AuditLogService auditLogService,
                                 ComplaintDataSupport complaintDataSupport) {
        this.complaintMapper = complaintMapper;
        this.auditLogService = auditLogService;
        this.complaintDataSupport = complaintDataSupport;
    }
    /**
     * 查询我的公共投诉
     * @param submitterUserId 提交用户ID
     * @param status 状态
     * @param page 页码
     * @param size 每页大小
     * @return 投诉列表
     */
    public PageResult<ComplaintListVO> listMyPublic(Long submitterUserId,
                                                    String status,
                                                    String keyword,
                                                    int page,
                                                    int size) {
        if (submitterUserId == null) {
            throw new IllegalArgumentException("unauthorized");
        }
        LambdaQueryWrapper<Complaint> wrapper = buildMyPublicWrapper(submitterUserId, status, keyword);
        wrapper.orderByDesc(Complaint::getUpdateTime);
        Page<Complaint> pageInfo = complaintMapper.selectPage(new Page<>(page, size), wrapper);
        List<Complaint> complaints = pageInfo.getRecords();
        Map<Long, String> enterpriseNames = complaintDataSupport.loadEnterpriseNames(complaints);
        Map<Long, String> handleResults = complaintDataSupport.loadHandleResults(complaints);
        List<ComplaintListVO> records = complaints.stream()
            .map(complaint -> complaintDataSupport.toListVO(complaint, enterpriseNames, handleResults))
            .toList();
        return PageResult.of(records, pageInfo.getTotal(), page, size);
    }

    public ComplaintPublicStatsVO statsMyPublic(Long submitterUserId, String status, String keyword) {
        if (submitterUserId == null) {
            throw new IllegalArgumentException("unauthorized");
        }
        ComplaintPublicStatsVO vo = new ComplaintPublicStatsVO();
        vo.setTotalCount(complaintMapper.selectCount(buildMyPublicWrapper(submitterUserId, status, keyword)));
        vo.setProcessingCount(complaintMapper.selectCount(buildMyPublicWrapper(submitterUserId, status, keyword)
            .in(Complaint::getStatus, List.of(ComplaintStatus.PENDING, ComplaintStatus.ASSIGNED, ComplaintStatus.PROCESSING))));
        vo.setFinishedCount(complaintMapper.selectCount(buildMyPublicWrapper(submitterUserId, status, keyword)
            .in(Complaint::getStatus, List.of(ComplaintStatus.FEEDBACKED, ComplaintStatus.REJECTED))));
        return vo;
    }

    private LambdaQueryWrapper<Complaint> buildMyPublicWrapper(Long submitterUserId, String status, String keyword) {
        LambdaQueryWrapper<Complaint> wrapper = new LambdaQueryWrapper<Complaint>()
            .eq(Complaint::getDeleted, 0)
            .eq(Complaint::getSubmitterUserId, submitterUserId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(Complaint::getStatus, complaintDataSupport.normalize(status));
        }
        if (StringUtils.hasText(keyword)) {
            String normalizedKeyword = keyword.trim();
            List<Long> enterpriseIds = complaintDataSupport.resolveEnterpriseIdsByName(normalizedKeyword);
            wrapper.and(item -> {
                item.like(Complaint::getComplaintNo, normalizedKeyword)
                    .or()
                    .like(Complaint::getContent, normalizedKeyword);
                if (enterpriseIds != null && !enterpriseIds.isEmpty()) {
                    item.or().in(Complaint::getEnterpriseId, enterpriseIds);
                }
            });
        }
        return wrapper;
    }
    /**
     * 获取我的公共投诉详情
     * @param submitterUserId 提交用户ID
     * @param complaintId 投诉ID
     * @return 投诉详情
     */
    public ComplaintVO getMyPublicDetail(Long submitterUserId, Long complaintId) {
        if (submitterUserId == null) {
            throw new IllegalArgumentException("unauthorized");
        }
        Complaint complaint = complaintDataSupport.requireComplaint(complaintId);
        if (!Objects.equals(complaint.getSubmitterUserId(), submitterUserId)) {
            throw new IllegalArgumentException("complaint not found");
        }
        return complaintDataSupport.toVOWithNames(complaint);
    }
    /**
     * 查询投诉列表
     * @param operatorUserId 操作员用户ID
     * @param status 状态
     * @param enterpriseName 企业名称
     * @param assignedToName 分配给名称
     * @param assignedByName 分配给用户名称
     * @param page 页码
     * @param size 每页大小
     * @return 投诉列表
     */
    public PageResult<ComplaintVO> list(Long operatorUserId,
                                        String complaintType,
                                        String status,
                                        String enterpriseName,
                                        String assignedToName,
                                        String assignedByName,
                                        int page,
                                        int size) {
        InternalRegulatorIdentityVO regulator = complaintDataSupport.requireRegulatorByUserId(operatorUserId);
        LambdaQueryWrapper<Complaint> wrapper = new LambdaQueryWrapper<Complaint>()
            .eq(Complaint::getDeleted, 0);
        if (StringUtils.hasText(complaintType)) {
            wrapper.eq(Complaint::getComplaintType, complaintDataSupport.normalizeComplaintType(complaintType));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Complaint::getStatus, complaintDataSupport.normalize(status));
        }
        if (ComplaintDataSupport.ROLE_ENFORCER.equalsIgnoreCase(regulator.getRoleType())) {
            wrapper.eq(Complaint::getAssignedTo, regulator.getId());
        }
        List<Long> enterpriseIds = complaintDataSupport.resolveEnterpriseIdsByName(enterpriseName);
        List<Long> scopeEnterpriseIds = complaintDataSupport.resolveEnterpriseIdsByRegion(regulator);
        if (scopeEnterpriseIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        if (enterpriseIds == null) {
            enterpriseIds = scopeEnterpriseIds;
        } else {
            enterpriseIds = enterpriseIds.stream().filter(scopeEnterpriseIds::contains).toList();
        }
        if (StringUtils.hasText(enterpriseName) && enterpriseIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        if (enterpriseIds != null) {
            wrapper.in(Complaint::getEnterpriseId, enterpriseIds);
        }
        List<Long> assignedToIds = complaintDataSupport.resolveRegulatorIdsByName(assignedToName);
        if (StringUtils.hasText(assignedToName) && assignedToIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        if (assignedToIds != null) {
            wrapper.in(Complaint::getAssignedTo, assignedToIds);
        }
        List<Long> assignedByIds = complaintDataSupport.resolveRegulatorIdsByName(assignedByName);
        if (StringUtils.hasText(assignedByName) && assignedByIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        if (assignedByIds != null) {
            wrapper.in(Complaint::getAssignedBy, assignedByIds);
        }
        wrapper.orderByDesc(Complaint::getUpdateTime);
        Page<Complaint> pageInfo = complaintMapper.selectPage(new Page<>(page, size), wrapper);
        List<Complaint> complaints = pageInfo.getRecords();
        Map<Long, String> enterpriseNames = complaintDataSupport.loadEnterpriseNames(complaints);
        Map<Long, String> regulatorNames = complaintDataSupport.loadRegulatorNames(complaints);
        Map<Long, String> handleResults = complaintDataSupport.loadHandleResults(complaints);
        List<ComplaintVO> records = complaints.stream()
            .map(complaint -> {
                ComplaintVO vo = complaintDataSupport.toVO(complaint, enterpriseNames, regulatorNames);
                vo.setHandleResult(handleResults.get(complaint.getId()));
                return vo;
            })
            .toList();
        return PageResult.of(records, pageInfo.getTotal(), page, size);
    }

    public InternalRegulatorIdentityVO requireRegulator(Long operatorUserId) {
        return complaintDataSupport.requireRegulatorByUserId(operatorUserId);
    }
    /**
     * 获取投诉详情
     * @param operatorUserId 操作员用户ID
     * @param complaintId 投诉ID
     * @return 投诉详情
     */
    public ComplaintDetailVO getDetail(Long operatorUserId, Long complaintId) {
        InternalRegulatorIdentityVO regulator = complaintDataSupport.requireRegulatorByUserId(operatorUserId);
        Complaint complaint = complaintDataSupport.requireComplaint(complaintId);
        if (!complaintDataSupport.isComplaintInRegion(regulator, complaint.getEnterpriseId())) {
            throw new IllegalArgumentException("complaint not in regulator region");
        }
        if (ComplaintDataSupport.ROLE_ENFORCER.equalsIgnoreCase(regulator.getRoleType())
            && !Objects.equals(complaint.getAssignedTo(), regulator.getId())) {
            throw new IllegalArgumentException("complaint not assigned to you");
        }
        InternalEnterpriseDetailVO enterprise = complaintDataSupport.requireEnterprise(complaint.getEnterpriseId());
        ComplaintDetailVO detail = new ComplaintDetailVO();
        detail.setComplaint(complaintDataSupport.toVOWithNames(complaint));
        detail.setEnterprise(complaintDataSupport.toEnterpriseProfileVO(enterprise));
        detail.setHandles(complaintDataSupport.loadHandleDetails(complaint.getId()));
        return detail;
    }

    public List<AuditLogVO> listLogs(Long operatorUserId, Long complaintId, int limit) {
        InternalRegulatorIdentityVO regulator = complaintDataSupport.requireRegulatorByUserId(operatorUserId);
        Complaint complaint = complaintDataSupport.requireComplaint(complaintId);
        if (!complaintDataSupport.isComplaintInRegion(regulator, complaint.getEnterpriseId())) {
            throw new IllegalArgumentException("complaint not in regulator region");
        }
        if (ComplaintDataSupport.ROLE_ENFORCER.equalsIgnoreCase(regulator.getRoleType())
            && !Objects.equals(complaint.getAssignedTo(), regulator.getId())) {
            throw new IllegalArgumentException("complaint not assigned to you");
        }
        return auditLogService.listComplaintLogs(complaintId, limit);
    }

    public List<AuditLogVO> listRecentLogs(Long operatorUserId, int limit) {
        InternalRegulatorIdentityVO regulator = complaintDataSupport.requireRegulatorByUserId(operatorUserId);
        List<Long> visibleComplaintIds = resolveVisibleComplaintIds(regulator, limit);
        return auditLogService.listRecentComplaintLogs(visibleComplaintIds, limit);
    }

    private List<Long> resolveVisibleComplaintIds(InternalRegulatorIdentityVO regulator, int limit) {
        int size = limit <= 0 ? 10 : Math.min(limit * 5, 200);
        LambdaQueryWrapper<Complaint> wrapper = new LambdaQueryWrapper<Complaint>()
            .eq(Complaint::getDeleted, 0)
            .orderByDesc(Complaint::getUpdateTime)
            .last("LIMIT " + size);
        if (ComplaintDataSupport.ROLE_ENFORCER.equalsIgnoreCase(regulator.getRoleType())) {
            wrapper.eq(Complaint::getAssignedTo, regulator.getId());
            return complaintMapper.selectList(wrapper).stream().map(Complaint::getId).toList();
        }
        List<Long> enterpriseIds = complaintDataSupport.resolveEnterpriseIdsByRegion(regulator);
        if (enterpriseIds.isEmpty()) {
            return List.of();
        }
        wrapper.in(Complaint::getEnterpriseId, enterpriseIds);
        return complaintMapper.selectList(wrapper).stream().map(Complaint::getId).toList();
    }
}

