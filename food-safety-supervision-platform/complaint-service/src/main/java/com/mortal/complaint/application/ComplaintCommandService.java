package com.mortal.complaint.application;

import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.util.StringUtils;

/**
 * 投诉命令服务
 */
@Service
public class ComplaintCommandService {
    /**
     * 默认截止时间（天）
     */
    private static final int DEFAULT_DEADLINE_DAYS = 3;
    /**
     * 关键原因类型：投诉超限
     */
    private static final String KEY_REASON_COMPLAINT_OVERFLOW = "COMPLAINT_OVERFLOW";
    /**
     * 关键原因来源：投诉
     */
    private static final String KEY_SOURCE_COMPLAINT = "COMPLAINT";

    private final ComplaintMapper complaintMapper;
    private final ComplaintDataSupport complaintDataSupport;

    /**
     * 投诉超限阈值
     */
    @Value("${complaint.key-supervision.threshold:3}")
    private int complaintOverflowThreshold = 3;

    /**
     * 投诉超限窗口时间（天）
     */
    @Value("${complaint.key-supervision.window-days:30}")
    private int complaintOverflowWindowDays = 30;

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
        tryMarkComplaintOverflowAsKey(complaint, regulator.getId());
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
        complaint.setDeadlineTime(resolveDeadlineTime(dto.getDeadlineTime()));
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
        String feedbackSummary = resolveFeedbackSummary(dto);
        String handleResult = resolveHandleResult(dto, feedbackSummary);
        complaintDataSupport.saveSingleHandle(complaint.getId(), regulator.getId(), handleResult);
        complaintDataSupport.transitionComplaint(complaint, ComplaintStatus.FEEDBACKED);
        complaint.setProcessedBy(regulator.getId());
        complaint.setProcessedTime(LocalDateTime.now());
        complaint.setFeedbackSummary(feedbackSummary);
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
        String rejectReason = complaintDataSupport.trim(dto.getReason());
        complaintDataSupport.transitionComplaint(complaint, ComplaintStatus.REJECTED);
        complaintDataSupport.saveSingleHandle(complaint.getId(), regulator.getId(), rejectReason);
        complaint.setRejectedBy(regulator.getId());
        complaint.setRejectedTime(LocalDateTime.now());
        complaint.setRejectReason(rejectReason);
        complaint.setUpdateTime(LocalDateTime.now());
        complaintMapper.updateById(complaint);
        return complaintDataSupport.toVOWithNames(complaint);
    }

    private LocalDateTime resolveDeadlineTime(LocalDateTime deadlineTime) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime resolved = deadlineTime == null ? now.plusDays(DEFAULT_DEADLINE_DAYS) : deadlineTime;
        if (!resolved.isAfter(now)) {
            throw new IllegalArgumentException("deadlineTime must be future");
        }
        return resolved;
    }
    /**
     * 解析反馈摘要
     * @param dto 投诉处理DTO
     * @return 反馈摘要
     */
    private String resolveFeedbackSummary(ComplaintHandleDTO dto) {
        String summary = complaintDataSupport.trim(dto.getFeedbackSummary());
        String handleResult = complaintDataSupport.trim(dto.getHandleResult());
        if (StringUtils.hasText(summary)) {
            return summary;
        }
        if (StringUtils.hasText(handleResult)) {
            return handleResult;
        }
        throw new IllegalArgumentException("feedbackSummary required");
    }

    private String resolveHandleResult(ComplaintHandleDTO dto, String feedbackSummary) {
        String handleResult = complaintDataSupport.trim(dto.getHandleResult());
        return StringUtils.hasText(handleResult) ? handleResult : feedbackSummary;
    }

    /**
     * 尝试标记投诉超限为关键企业
     * @param complaint 投诉
     * @param operatorId 操作员ID
     */
    private void tryMarkComplaintOverflowAsKey(Complaint complaint, Long operatorId) {
        if (complaint == null || complaint.getEnterpriseId() == null || complaint.getId() == null) {
            return;
        }
        int safeThreshold = Math.max(1, complaintOverflowThreshold);
        int safeWindowDays = Math.max(1, complaintOverflowWindowDays);
        long acceptedCount = complaintDataSupport.countAcceptedComplaints(
            complaint.getEnterpriseId(),
            LocalDateTime.now().minusDays(safeWindowDays)
        );
        if (acceptedCount < safeThreshold) {
            return;
        }
        String reasonDetail = "企业近" + safeWindowDays + "天有效投诉达到" + acceptedCount + "件，已自动纳入重点监管";
        complaintDataSupport.markEnterpriseAsKey(
            complaint.getEnterpriseId(),
            KEY_REASON_COMPLAINT_OVERFLOW,
            reasonDetail,
            KEY_SOURCE_COMPLAINT,
            complaint.getId(),
            operatorId
        );
    }
}
