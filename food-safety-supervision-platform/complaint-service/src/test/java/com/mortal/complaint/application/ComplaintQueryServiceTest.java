package com.mortal.complaint.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mortal.complaint.client.regulation.vo.InternalEnterpriseDetailVO;
import com.mortal.complaint.client.regulation.vo.InternalRegulatorIdentityVO;
import com.mortal.complaint.domain.entity.Complaint;
import com.mortal.complaint.domain.enums.ComplaintStatus;
import com.mortal.complaint.infrastructure.mapper.ComplaintMapper;
import com.mortal.complaint.vo.ComplaintDetailVO;
import com.mortal.complaint.vo.ComplaintVO;
import org.junit.jupiter.api.Test;

class ComplaintQueryServiceTest {

    @Test
    void getMyPublicDetail_shouldReturnComplaintForSubmitter() {
        ComplaintMapper complaintMapper = mock(ComplaintMapper.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        ComplaintDataSupport complaintDataSupport = mock(ComplaintDataSupport.class);
        ComplaintQueryService service = new ComplaintQueryService(complaintMapper, auditLogService, complaintDataSupport);

        Complaint complaint = new Complaint();
        complaint.setId(100L);
        complaint.setSubmitterUserId(14L);
        ComplaintVO vo = new ComplaintVO();
        vo.setId(100L);
        vo.setAnonymous(true);

        when(complaintDataSupport.requireComplaint(100L)).thenReturn(complaint);
        when(complaintDataSupport.toVOWithNames(complaint)).thenReturn(vo);

        ComplaintVO result = service.getMyPublicDetail(14L, 100L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(Boolean.TRUE, result.getAnonymous());
    }

    @Test
    void getMyPublicDetail_shouldRejectOtherUser() {
        ComplaintMapper complaintMapper = mock(ComplaintMapper.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        ComplaintDataSupport complaintDataSupport = mock(ComplaintDataSupport.class);
        ComplaintQueryService service = new ComplaintQueryService(complaintMapper, auditLogService, complaintDataSupport);

        Complaint complaint = new Complaint();
        complaint.setId(100L);
        complaint.setSubmitterUserId(14L);

        when(complaintDataSupport.requireComplaint(100L)).thenReturn(complaint);

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.getMyPublicDetail(15L, 100L)
        );

        assertEquals("complaint not found", error.getMessage());
    }

    @Test
    void getDetail_shouldReturnComplaintForScopedRegulator() {
        ComplaintMapper complaintMapper = mock(ComplaintMapper.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        ComplaintDataSupport complaintDataSupport = mock(ComplaintDataSupport.class);
        ComplaintQueryService service = new ComplaintQueryService(complaintMapper, auditLogService, complaintDataSupport);

        InternalRegulatorIdentityVO regulator = new InternalRegulatorIdentityVO();
        regulator.setId(3L);
        regulator.setRoleType(ComplaintDataSupport.ROLE_ADMIN);

        Complaint complaint = new Complaint();
        complaint.setId(200L);
        complaint.setEnterpriseId(8L);
        complaint.setAssignedTo(6L);
        complaint.setStatus(ComplaintStatus.PROCESSING);

        ComplaintVO complaintVO = new ComplaintVO();
        complaintVO.setId(200L);
        complaintVO.setComplainantName("张三");
        complaintVO.setContactMasked("138****1234");

        InternalEnterpriseDetailVO enterprise = new InternalEnterpriseDetailVO();
        enterprise.setId(8L);
        enterprise.setEnterpriseName("香满楼餐厅");

        when(complaintDataSupport.requireRegulatorByUserId(1L)).thenReturn(regulator);
        when(complaintDataSupport.requireComplaint(200L)).thenReturn(complaint);
        when(complaintDataSupport.isComplaintInRegion(regulator, 8L)).thenReturn(true);
        when(complaintDataSupport.requireEnterprise(8L)).thenReturn(enterprise);
        when(complaintDataSupport.toVOWithNames(complaint)).thenReturn(complaintVO);
        when(complaintDataSupport.toEnterpriseProfileVO(enterprise)).thenReturn(null);
        when(complaintDataSupport.loadHandleDetails(200L)).thenReturn(java.util.List.of());

        ComplaintDetailVO result = service.getDetail(1L, 200L);

        assertNotNull(result);
        assertNotNull(result.getComplaint());
        assertEquals("张三", result.getComplaint().getComplainantName());
        assertEquals("138****1234", result.getComplaint().getContactMasked());
    }
}
