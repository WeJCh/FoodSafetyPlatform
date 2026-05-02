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
import com.mortal.complaint.support.ComplaintLockSupport;
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
    private final ComplaintLockSupport complaintLockSupport;
    private final AuditLogService auditLogService;

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
                                   ComplaintDataSupport complaintDataSupport,
                                   ComplaintLockSupport complaintLockSupport,
                                   AuditLogService auditLogService) {
        this.complaintMapper = complaintMapper;
        this.complaintDataSupport = complaintDataSupport;
        this.complaintLockSupport = complaintLockSupport;
        this.auditLogService = auditLogService;
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
        auditLogService.recordComplaintAudit(
            submitterUserId,
            "PUBLIC",
            null,
            "COMPLAINT_SUBMIT",
            "提交投诉",
            null,
            copyComplaint(complaint),
                "公众用户提交投诉，当前状态为待受理");
        return complaintDataSupport.toTrackVO(complaint);
    }

    /**
     * 接受投诉
     * @param operatorUserId 操作员用户ID
     * @param complaintId 投诉ID
     * @return 投诉VO
     */
    public ComplaintVO accept(Long operatorUserId, Long complaintId) {
        return complaintLockSupport.executeWithLock("complaint-action", complaintId, () -> {
            InternalRegulatorIdentityVO regulator = complaintDataSupport.requireRegulatorByUserId(operatorUserId);
            complaintDataSupport.requireRole(regulator, ComplaintDataSupport.ROLE_ADMIN);
            Complaint complaint = complaintDataSupport.requireComplaint(complaintId);
            Complaint beforeComplaint = copyComplaint(complaint);
            complaintDataSupport.transitionComplaint(complaint, ComplaintStatus.PENDING);
            complaint.setAcceptedBy(regulator.getId());
            complaint.setAcceptedTime(LocalDateTime.now());
            complaint.setUpdateTime(LocalDateTime.now());
            complaintMapper.updateById(complaint);
            tryMarkComplaintOverflowAsKey(complaint, regulator.getId());
            auditLogService.recordComplaintAudit(
                regulator.getUserId(),
                regulator.getRoleType(),
                regulator.getName(),
                "COMPLAINT_ACCEPT",
                "受理投诉",
                beforeComplaint,
                copyComplaint(complaint),
                "投诉已受理，状态由待受理调整为待分派");
            return complaintDataSupport.toVOWithNames(complaint);
        });
    }

    /**
     * 分配投诉
     * @param operatorUserId 操作员用户ID
     * @param complaintId 投诉ID
     * @param dto 投诉分配DTO
     * @return 投诉VO
     */
    public ComplaintVO assign(Long operatorUserId, Long complaintId, ComplaintAssignDTO dto) {
        return complaintLockSupport.executeWithLock("complaint-action", complaintId, () -> {
            InternalRegulatorIdentityVO regulator = complaintDataSupport.requireRegulatorByUserId(operatorUserId);
            complaintDataSupport.requireRole(regulator, ComplaintDataSupport.ROLE_ADMIN);
            Complaint complaint = complaintDataSupport.requireComplaint(complaintId);
            Complaint beforeComplaint = copyComplaint(complaint);
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
            auditLogService.recordComplaintAudit(
                regulator.getUserId(),
                regulator.getRoleType(),
                regulator.getName(),
                "COMPLAINT_ASSIGN",
                "分派投诉",
                beforeComplaint,
                copyComplaint(complaint),
                "投诉已分派给" + assignee.getName() + "，状态调整为已分派");
            return complaintDataSupport.toVOWithNames(complaint);
        });
    }

    /**
     * 开始处理投诉
     * @param operatorUserId 操作员用户ID
     * @param complaintId 投诉ID
     * @return 投诉VO
     */
    public ComplaintVO startProcess(Long operatorUserId, Long complaintId) {
        return complaintLockSupport.executeWithLock("complaint-action", complaintId, () -> {
            InternalRegulatorIdentityVO regulator = complaintDataSupport.requireRegulatorByUserId(operatorUserId);
            complaintDataSupport.requireRole(regulator, ComplaintDataSupport.ROLE_ENFORCER);
            Complaint complaint = complaintDataSupport.requireComplaint(complaintId);
            Complaint beforeComplaint = copyComplaint(complaint);
            if (!Objects.equals(complaint.getAssignedTo(), regulator.getId())) {
                throw new IllegalArgumentException("complaint not assigned to you");
            }
            complaintDataSupport.transitionComplaint(complaint, ComplaintStatus.PROCESSING);
            complaint.setUpdateTime(LocalDateTime.now());
            complaintMapper.updateById(complaint);
            auditLogService.recordComplaintAudit(
                regulator.getUserId(),
                regulator.getRoleType(),
                regulator.getName(),
                "COMPLAINT_PROCESS_START",
                "开始处理投诉",
                beforeComplaint,
                copyComplaint(complaint),
                "投诉开始处理，状态由已分派调整为处理中");
            return complaintDataSupport.toVOWithNames(complaint);
        });
    }

    /**
     * 处理投诉
     * @param operatorUserId 操作员用户ID
     * @param complaintId 投诉ID
     * @param dto 投诉处理DTO
     * @return 投诉VO
     */
    public ComplaintVO handle(Long operatorUserId, Long complaintId, ComplaintHandleDTO dto) {
        return complaintLockSupport.executeWithLock("complaint-action", complaintId, () -> {
            InternalRegulatorIdentityVO regulator = complaintDataSupport.requireRegulatorByUserId(operatorUserId);
            complaintDataSupport.requireRole(regulator, ComplaintDataSupport.ROLE_ENFORCER);
            Complaint complaint = complaintDataSupport.requireComplaint(complaintId);
            Complaint beforeComplaint = copyComplaint(complaint);
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
            auditLogService.recordComplaintAudit(
                regulator.getUserId(),
                regulator.getRoleType(),
                regulator.getName(),
                "COMPLAINT_HANDLE",
                "处理完成投诉",
                beforeComplaint,
                copyComplaint(complaint),
                "投诉处理完成，状态调整为已反馈");
            return complaintDataSupport.toVOWithNames(complaint);
        });
    }

    /**
     * 驳回投诉
     * @param operatorUserId 操作员用户ID
     * @param complaintId 投诉ID
     * @param dto 驳回原因
     * @return 投诉VO
     */
    public ComplaintVO reject(Long operatorUserId, Long complaintId, ComplaintRejectDTO dto) {
        return complaintLockSupport.executeWithLock("complaint-action", complaintId, () -> {
            InternalRegulatorIdentityVO regulator = complaintDataSupport.requireRegulatorByUserId(operatorUserId);
            complaintDataSupport.requireRole(regulator, ComplaintDataSupport.ROLE_ADMIN);
            Complaint complaint = complaintDataSupport.requireComplaint(complaintId);
            Complaint beforeComplaint = copyComplaint(complaint);
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
            auditLogService.recordComplaintAudit(
                regulator.getUserId(),
                regulator.getRoleType(),
                regulator.getName(),
                "COMPLAINT_REJECT",
                "驳回投诉",
                beforeComplaint,
                copyComplaint(complaint),
                "投诉已驳回，状态调整为已驳回");
            return complaintDataSupport.toVOWithNames(complaint);
        });
    }

    private Complaint copyComplaint(Complaint source) {
        if (source == null) {
            return null;
        }
        Complaint target = new Complaint();
        target.setId(source.getId());
        target.setComplaintNo(source.getComplaintNo());
        target.setComplainantName(source.getComplainantName());
        target.setContact(source.getContact());
        target.setSubmitterUserId(source.getSubmitterUserId());
        target.setEnterpriseId(source.getEnterpriseId());
        target.setComplaintType(source.getComplaintType());
        target.setContent(source.getContent());
        target.setImageUrls(source.getImageUrls());
        target.setStatus(source.getStatus());
        target.setSourceType(source.getSourceType());
        target.setSourceId(source.getSourceId());
        target.setAssignedTo(source.getAssignedTo());
        target.setAssignedBy(source.getAssignedBy());
        target.setAssignedTime(source.getAssignedTime());
        target.setDeadlineTime(source.getDeadlineTime());
        target.setAcceptedBy(source.getAcceptedBy());
        target.setAcceptedTime(source.getAcceptedTime());
        target.setProcessedBy(source.getProcessedBy());
        target.setProcessedTime(source.getProcessedTime());
        target.setFeedbackSummary(source.getFeedbackSummary());
        target.setRejectedBy(source.getRejectedBy());
        target.setRejectedTime(source.getRejectedTime());
        target.setRejectReason(source.getRejectReason());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setDeleted(source.getDeleted());
        return target;
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
