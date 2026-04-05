package com.mortal.complaint.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mortal.complaint.client.regulation.vo.InternalRegulatorIdentityVO;
import com.mortal.complaint.client.regulation.vo.InternalRegulatorSummaryVO;
import com.mortal.complaint.domain.entity.Complaint;
import com.mortal.complaint.domain.enums.ComplaintStatus;
import com.mortal.complaint.dto.ComplaintAssignDTO;
import com.mortal.complaint.dto.ComplaintHandleDTO;
import com.mortal.complaint.dto.ComplaintRejectDTO;
import com.mortal.complaint.infrastructure.mapper.ComplaintMapper;
import com.mortal.complaint.vo.ComplaintVO;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ComplaintCommandServiceTest {

    @Test
    void assign_shouldApplyDefaultDeadlineWhenMissing() {
        ComplaintMapper complaintMapper = mock(ComplaintMapper.class);
        ComplaintDataSupport complaintDataSupport = mock(ComplaintDataSupport.class);
        ComplaintCommandService service = new ComplaintCommandService(complaintMapper, complaintDataSupport);

        InternalRegulatorIdentityVO admin = new InternalRegulatorIdentityVO();
        admin.setId(10L);
        admin.setRoleType(ComplaintDataSupport.ROLE_ADMIN);
        Complaint complaint = new Complaint();
        complaint.setId(100L);
        complaint.setStatus(ComplaintStatus.PENDING);
        InternalRegulatorSummaryVO enforcer = new InternalRegulatorSummaryVO();
        enforcer.setId(20L);
        enforcer.setRoleType(ComplaintDataSupport.ROLE_ENFORCER);
        ComplaintVO result = new ComplaintVO();

        when(complaintDataSupport.requireRegulatorByUserId(1L)).thenReturn(admin);
        when(complaintDataSupport.requireComplaint(100L)).thenReturn(complaint);
        when(complaintDataSupport.requireRegulatorById(9L, "assignee not found")).thenReturn(enforcer);
        when(complaintDataSupport.toVOWithNames(complaint)).thenReturn(result);
        doAnswer(invocation -> {
            Complaint target = invocation.getArgument(0);
            ComplaintStatus status = invocation.getArgument(1);
            target.setStatus(status);
            return null;
        }).when(complaintDataSupport).transitionComplaint(any(Complaint.class), any(ComplaintStatus.class));

        ComplaintAssignDTO dto = new ComplaintAssignDTO();
        dto.setRegulatorId(9L);
        LocalDateTime before = LocalDateTime.now();

        ComplaintVO response = service.assign(1L, 100L, dto);

        LocalDateTime after = LocalDateTime.now();
        assertNotNull(response);
        assertEquals(ComplaintStatus.ASSIGNED, complaint.getStatus());
        assertEquals(20L, complaint.getAssignedTo());
        assertNotNull(complaint.getDeadlineTime());
        assertTrue(!complaint.getDeadlineTime().isBefore(before.plusDays(3)));
        assertTrue(!complaint.getDeadlineTime().isAfter(after.plusDays(3).plusSeconds(1)));
        verify(complaintMapper).updateById(complaint);
    }

    @Test
    void handle_shouldPersistFeedbackSummaryAndAuditResult() {
        ComplaintMapper complaintMapper = mock(ComplaintMapper.class);
        ComplaintDataSupport complaintDataSupport = mock(ComplaintDataSupport.class);
        ComplaintCommandService service = new ComplaintCommandService(complaintMapper, complaintDataSupport);

        InternalRegulatorIdentityVO enforcer = new InternalRegulatorIdentityVO();
        enforcer.setId(20L);
        enforcer.setRoleType(ComplaintDataSupport.ROLE_ENFORCER);
        Complaint complaint = new Complaint();
        complaint.setId(100L);
        complaint.setAssignedTo(20L);
        complaint.setStatus(ComplaintStatus.PROCESSING);
        ComplaintVO result = new ComplaintVO();

        when(complaintDataSupport.requireRegulatorByUserId(2L)).thenReturn(enforcer);
        when(complaintDataSupport.requireComplaint(100L)).thenReturn(complaint);
        when(complaintDataSupport.trim("已完成核查，责令门店下架整改")).thenReturn("已完成核查，责令门店下架整改");
        when(complaintDataSupport.trim(null)).thenReturn(null);
        when(complaintDataSupport.toVOWithNames(complaint)).thenReturn(result);
        doAnswer(invocation -> {
            Complaint target = invocation.getArgument(0);
            ComplaintStatus status = invocation.getArgument(1);
            target.setStatus(status);
            return null;
        }).when(complaintDataSupport).transitionComplaint(any(Complaint.class), any(ComplaintStatus.class));

        ComplaintHandleDTO dto = new ComplaintHandleDTO();
        dto.setFeedbackSummary("已完成核查，责令门店下架整改");

        ComplaintVO response = service.handle(2L, 100L, dto);

        assertNotNull(response);
        assertEquals(ComplaintStatus.FEEDBACKED, complaint.getStatus());
        assertEquals("已完成核查，责令门店下架整改", complaint.getFeedbackSummary());
        assertEquals(20L, complaint.getProcessedBy());
        verify(complaintDataSupport).saveSingleHandle(100L, 20L, "已完成核查，责令门店下架整改");
        verify(complaintMapper).updateById(complaint);
    }

    @Test
    void reject_shouldPersistRejectReasonAndAuditResult() {
        ComplaintMapper complaintMapper = mock(ComplaintMapper.class);
        ComplaintDataSupport complaintDataSupport = mock(ComplaintDataSupport.class);
        ComplaintCommandService service = new ComplaintCommandService(complaintMapper, complaintDataSupport);

        InternalRegulatorIdentityVO admin = new InternalRegulatorIdentityVO();
        admin.setId(10L);
        admin.setRoleType(ComplaintDataSupport.ROLE_ADMIN);
        Complaint complaint = new Complaint();
        complaint.setId(100L);
        complaint.setEnterpriseId(300L);
        complaint.setStatus(ComplaintStatus.PENDING);
        ComplaintVO result = new ComplaintVO();

        when(complaintDataSupport.requireRegulatorByUserId(1L)).thenReturn(admin);
        when(complaintDataSupport.requireComplaint(100L)).thenReturn(complaint);
        when(complaintDataSupport.isComplaintInRegion(admin, 300L)).thenReturn(true);
        when(complaintDataSupport.trim("投诉信息不完整，无法核实")).thenReturn("投诉信息不完整，无法核实");
        when(complaintDataSupport.toVOWithNames(complaint)).thenReturn(result);
        doAnswer(invocation -> {
            Complaint target = invocation.getArgument(0);
            ComplaintStatus status = invocation.getArgument(1);
            target.setStatus(status);
            return null;
        }).when(complaintDataSupport).transitionComplaint(any(Complaint.class), any(ComplaintStatus.class));

        ComplaintRejectDTO dto = new ComplaintRejectDTO();
        dto.setReason("投诉信息不完整，无法核实");

        ComplaintVO response = service.reject(1L, 100L, dto);

        assertNotNull(response);
        assertEquals(ComplaintStatus.REJECTED, complaint.getStatus());
        assertEquals("投诉信息不完整，无法核实", complaint.getRejectReason());
        assertEquals(10L, complaint.getRejectedBy());
        verify(complaintDataSupport).saveSingleHandle(100L, 10L, "投诉信息不完整，无法核实");
        verify(complaintMapper).updateById(complaint);
    }

    @Test
    void accept_shouldMarkEnterpriseAsKeyWhenComplaintOverflowTriggered() {
        ComplaintMapper complaintMapper = mock(ComplaintMapper.class);
        ComplaintDataSupport complaintDataSupport = mock(ComplaintDataSupport.class);
        ComplaintCommandService service = new ComplaintCommandService(complaintMapper, complaintDataSupport);

        InternalRegulatorIdentityVO admin = new InternalRegulatorIdentityVO();
        admin.setId(10L);
        admin.setRoleType(ComplaintDataSupport.ROLE_ADMIN);
        Complaint complaint = new Complaint();
        complaint.setId(100L);
        complaint.setEnterpriseId(300L);
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        ComplaintVO result = new ComplaintVO();

        when(complaintDataSupport.requireRegulatorByUserId(1L)).thenReturn(admin);
        when(complaintDataSupport.requireComplaint(100L)).thenReturn(complaint);
        when(complaintDataSupport.countAcceptedComplaints(any(), any())).thenReturn(3L);
        when(complaintDataSupport.toVOWithNames(complaint)).thenReturn(result);
        doAnswer(invocation -> {
            Complaint target = invocation.getArgument(0);
            ComplaintStatus status = invocation.getArgument(1);
            target.setStatus(status);
            return null;
        }).when(complaintDataSupport).transitionComplaint(any(Complaint.class), any(ComplaintStatus.class));

        ComplaintVO response = service.accept(1L, 100L);

        assertNotNull(response);
        assertEquals(ComplaintStatus.PENDING, complaint.getStatus());
        verify(complaintDataSupport).markEnterpriseAsKey(
            300L,
            "COMPLAINT_OVERFLOW",
            "企业近30天有效投诉达到3件，已自动纳入重点监管",
            "COMPLAINT",
            100L,
            10L
        );
        verify(complaintMapper).updateById(complaint);
    }
}
