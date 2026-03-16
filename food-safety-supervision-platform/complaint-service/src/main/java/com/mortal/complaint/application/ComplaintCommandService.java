package com.mortal.complaint.application;

import com.mortal.complaint.client.regulation.vo.InternalEnterpriseDetailVO;
import com.mortal.complaint.client.regulation.vo.InternalRegulatorIdentityVO;
import com.mortal.complaint.client.regulation.vo.InternalRegulatorSummaryVO;
import com.mortal.complaint.domain.entity.Complaint;
import com.mortal.complaint.domain.enums.ComplaintStatus;
import com.mortal.complaint.domain.enums.TaskSourceType;
import com.mortal.complaint.dto.ComplaintAssignDTO;
import com.mortal.complaint.dto.ComplaintHandleDTO;
import com.mortal.complaint.dto.ComplaintRejectDTO;
import com.mortal.complaint.dto.ComplaintSubmitDTO;
import com.mortal.complaint.infrastructure.mapper.ComplaintMapper;
import com.mortal.complaint.vo.ComplaintTrackVO;
import com.mortal.complaint.vo.ComplaintVO;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 投诉命令服务
 */
@Service
public class ComplaintCommandService {

    private final ComplaintMapper complaintMapper;
    private final ComplaintDataSupport complaintDataSupport;

    /**
     * 构造函数
     * @param complaintMapper 投诉Mapper
     * @param complaintDataSupport 投诉数据支持
     */
    public ComplaintCommandService(ComplaintMapper complaintMapper,
                                   ComplaintDataSupport complaintDataSupport) {
        this.complaintMapper = complaintMapper;
        this.complaintDataSupport = complaintDataSupport;
    }

    /**
     * 提交公共投诉
     * @param submitterUserId 提交用户ID
     * @param dto 投诉提交DTO
     * @return 投诉跟踪VO
     */
    public ComplaintTrackVO submitPublic(Long submitterUserId, ComplaintSubmitDTO dto) {
        InternalEnterpriseDetailVO enterprise = complaintDataSupport.requireEnterprise(dto.getEnterpriseId());
        Complaint complaint = new Complaint();
        complaint.setComplaintNo(complaintDataSupport.generateComplaintNo());
        complaint.setComplainantName(complaintDataSupport.trim(dto.getComplainantName()));
        complaint.setContact(complaintDataSupport.trim(dto.getContact()));
        complaint.setSubmitterUserId(submitterUserId);
        complaint.setEnterpriseId(enterprise.getId());
        complaint.setComplaintType(complaintDataSupport.trim(dto.getComplaintType()));
        complaint.setContent(dto.getContent().trim());
        complaint.setImageUrls(complaintDataSupport.serializeImageUrls(dto.getImageUrls()));
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        complaint.setSourceType(TaskSourceType.MANUAL);
        complaint.setCreateTime(LocalDateTime.now());
        complaint.setUpdateTime(LocalDateTime.now());
        complaint.setDeleted(0);
        complaintMapper.insert(complaint);
        return complaintDataSupport.toTrackVO(complaint);
    }

    /**
     * 接受投诉
     * @param operatorUserId 操作员用户ID
     * @param complaintId 投诉ID
     * @return 投诉VO
     */
    public ComplaintVO accept(Long operatorUserId, Long complaintId) {
        InternalRegulatorIdentityVO regulator = complaintDataSupport.requireRegulatorByUserId(operatorUserId);
        complaintDataSupport.requireRole(regulator, ComplaintDataSupport.ROLE_ADMIN);
        Complaint complaint = complaintDataSupport.requireComplaint(complaintId);
        complaintDataSupport.transitionComplaint(complaint, ComplaintStatus.PENDING);
        complaint.setAcceptedBy(regulator.getId());
        complaint.setAcceptedTime(LocalDateTime.now());
        complaint.setUpdateTime(LocalDateTime.now());
        complaintMapper.updateById(complaint);
        return complaintDataSupport.toVOWithNames(complaint);
    }

    /**
     * 分配投诉
     * @param operatorUserId 操作员用户ID
     * @param complaintId 投诉ID
     * @param dto 投诉分配DTO
     * @return 投诉VO
     */
    public ComplaintVO assign(Long operatorUserId, Long complaintId, ComplaintAssignDTO dto) {
        InternalRegulatorIdentityVO regulator = complaintDataSupport.requireRegulatorByUserId(operatorUserId);
        complaintDataSupport.requireRole(regulator, ComplaintDataSupport.ROLE_ADMIN);
        Complaint complaint = complaintDataSupport.requireComplaint(complaintId);
        if (!ComplaintStatus.PENDING.equals(complaint.getStatus())
            && !ComplaintStatus.ASSIGNED.equals(complaint.getStatus())
            && !ComplaintStatus.PROCESSING.equals(complaint.getStatus())) {
            throw new IllegalArgumentException("complaint not ready for assignment");
        }
        InternalRegulatorSummaryVO assignee =
            complaintDataSupport.requireRegulatorById(dto.getRegulatorId(), "assignee not found");
        if (!ComplaintDataSupport.ROLE_ENFORCER.equalsIgnoreCase(assignee.getRoleType())) {
            throw new IllegalArgumentException("assignee must be enforcer");
        }
        complaint.setAssignedTo(assignee.getId());
        complaint.setAssignedBy(regulator.getId());
        complaint.setAssignedTime(LocalDateTime.now());
        complaintDataSupport.transitionComplaint(complaint, ComplaintStatus.ASSIGNED);
        complaint.setUpdateTime(LocalDateTime.now());
        complaintMapper.updateById(complaint);
        return complaintDataSupport.toVOWithNames(complaint);
    }

    /**
     * 开始处理投诉
     * @param operatorUserId 操作员用户ID
     * @param complaintId 投诉ID
     * @return 投诉VO
     */
    public ComplaintVO startProcess(Long operatorUserId, Long complaintId) {
        InternalRegulatorIdentityVO regulator = complaintDataSupport.requireRegulatorByUserId(operatorUserId);
        complaintDataSupport.requireRole(regulator, ComplaintDataSupport.ROLE_ENFORCER);
        Complaint complaint = complaintDataSupport.requireComplaint(complaintId);
        if (!Objects.equals(complaint.getAssignedTo(), regulator.getId())) {
            throw new IllegalArgumentException("complaint not assigned to you");
        }
        complaintDataSupport.transitionComplaint(complaint, ComplaintStatus.PROCESSING);
        complaint.setUpdateTime(LocalDateTime.now());
        complaintMapper.updateById(complaint);
        return complaintDataSupport.toVOWithNames(complaint);
    }

    /**
     * 处理投诉
     * @param operatorUserId 操作员用户ID
     * @param complaintId 投诉ID
     * @param dto 投诉处理DTO
     * @return 投诉VO
     */
    public ComplaintVO handle(Long operatorUserId, Long complaintId, ComplaintHandleDTO dto) {
        InternalRegulatorIdentityVO regulator = complaintDataSupport.requireRegulatorByUserId(operatorUserId);
        complaintDataSupport.requireRole(regulator, ComplaintDataSupport.ROLE_ENFORCER);
        Complaint complaint = complaintDataSupport.requireComplaint(complaintId);
        if (!Objects.equals(complaint.getAssignedTo(), regulator.getId())) {
            throw new IllegalArgumentException("complaint not assigned to you");
        }
        if (!ComplaintStatus.PROCESSING.equals(complaint.getStatus())) {
            throw new IllegalArgumentException("complaint not in processing");
        }
        complaintDataSupport.saveSingleHandle(complaint.getId(), regulator.getId(), dto.getHandleResult().trim());
        complaintDataSupport.transitionComplaint(complaint, ComplaintStatus.FEEDBACKED);
        complaint.setProcessedBy(regulator.getId());
        complaint.setProcessedTime(LocalDateTime.now());
        complaint.setUpdateTime(LocalDateTime.now());
        complaintMapper.updateById(complaint);
        return complaintDataSupport.toVOWithNames(complaint);
    }

    /**
     * 驳回投诉
     * @param operatorUserId 操作员用户ID
     * @param complaintId 投诉ID
     * @param dto 驳回原因
     * @return 投诉VO
     */
    public ComplaintVO reject(Long operatorUserId, Long complaintId, ComplaintRejectDTO dto) {
        InternalRegulatorIdentityVO regulator = complaintDataSupport.requireRegulatorByUserId(operatorUserId);
        complaintDataSupport.requireRole(regulator, ComplaintDataSupport.ROLE_ADMIN);
        Complaint complaint = complaintDataSupport.requireComplaint(complaintId);
        if (!complaintDataSupport.isComplaintInRegion(regulator, complaint.getEnterpriseId())) {
            throw new IllegalArgumentException("complaint not in regulator region");
        }
        complaintDataSupport.transitionComplaint(complaint, ComplaintStatus.REJECTED);
        complaintDataSupport.saveSingleHandle(complaint.getId(), regulator.getId(), dto.getReason().trim());
        complaint.setRejectedBy(regulator.getId());
        complaint.setRejectedTime(LocalDateTime.now());
        complaint.setUpdateTime(LocalDateTime.now());
        complaintMapper.updateById(complaint);
        return complaintDataSupport.toVOWithNames(complaint);
    }
}
