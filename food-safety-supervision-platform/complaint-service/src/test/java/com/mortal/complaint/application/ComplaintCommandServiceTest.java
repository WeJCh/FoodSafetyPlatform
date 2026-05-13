package com.mortal.complaint.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mortal.complaint.client.regulation.vo.InternalEnterpriseDetailVO;
import com.mortal.complaint.client.regulation.vo.InternalRegulatorIdentityVO;
import com.mortal.complaint.client.regulation.vo.InternalRegulatorSummaryVO;
import com.mortal.complaint.client.user.vo.UserVO;
import com.mortal.complaint.domain.entity.Complaint;
import com.mortal.complaint.domain.enums.ComplaintStatus;
import com.mortal.complaint.dto.ComplaintAssignDTO;
import com.mortal.complaint.dto.ComplaintSubmitDTO;
import com.mortal.complaint.infrastructure.mapper.ComplaintMapper;
import com.mortal.complaint.support.ComplaintLockSupport;
import com.mortal.complaint.vo.ComplaintTrackVO;
import com.mortal.complaint.vo.ComplaintVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ComplaintCommandServiceTest {

    @Test
    void submitPublic_shouldSnapshotPublicUserForAnonymousComplaint() {
        ComplaintMapper complaintMapper = mock(ComplaintMapper.class);
        ComplaintDataSupport complaintDataSupport = mock(ComplaintDataSupport.class);
        ComplaintLockSupport complaintLockSupport = mock(ComplaintLockSupport.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        ComplaintAuditOperatorNameResolver resolver = new ComplaintAuditOperatorNameResolver();
        ComplaintCommandService service = new ComplaintCommandService(
            complaintMapper,
            complaintDataSupport,
            complaintLockSupport,
            auditLogService,
            resolver
        );

        InternalEnterpriseDetailVO enterprise = new InternalEnterpriseDetailVO();
        enterprise.setId(9L);
        UserVO submitter = new UserVO();
        submitter.setRealName("张三");
        submitter.setPhone("13800001234");
        ComplaintTrackVO track = new ComplaintTrackVO();
        track.setComplaintNo("CPT202605060001");
        AtomicReference<Complaint> insertedComplaint = new AtomicReference<>();

        when(complaintDataSupport.requireEnterprise(9L)).thenReturn(enterprise);
        when(complaintDataSupport.requirePublicUserById(14L)).thenReturn(submitter);
        when(complaintDataSupport.generateComplaintNo()).thenReturn("CPT202605060001");
        when(complaintDataSupport.trim("张三")).thenReturn("张三");
        when(complaintDataSupport.trim("13800001234")).thenReturn("13800001234");
        when(complaintDataSupport.trim("FOOD_SAFETY")).thenReturn("FOOD_SAFETY");
        when(complaintDataSupport.normalizeComplaintType("FOOD_SAFETY")).thenReturn("FOOD_SAFETY");
        when(complaintDataSupport.serializeImageUrls(List.of("https://file.example.com/a.jpg")))
            .thenReturn("[\"https://file.example.com/a.jpg\"]");
        when(complaintDataSupport.toTrackVO(any(Complaint.class))).thenReturn(track);
        doAnswer(invocation -> {
            Complaint complaint = invocation.getArgument(0);
            insertedComplaint.set(complaint);
            complaint.setId(100L);
            return 1;
        }).when(complaintMapper).insert(any(Complaint.class));

        ComplaintSubmitDTO dto = new ComplaintSubmitDTO();
        dto.setEnterpriseId(9L);
        dto.setAnonymous(true);
        dto.setComplaintType("FOOD_SAFETY");
        dto.setContent("发现菜品异物");
        dto.setImageUrls(List.of("https://file.example.com/a.jpg"));

        ComplaintTrackVO result = service.submitPublic(14L, dto);

        assertNotNull(result);
        Complaint saved = insertedComplaint.get();
        assertNotNull(saved);
        assertEquals("张三", saved.getComplainantName());
        assertEquals("13800001234", saved.getContact());
        assertEquals(14L, saved.getSubmitterUserId());
        assertEquals(1, saved.getAnonymousFlag());
        assertEquals("FOOD_SAFETY", saved.getComplaintType());
        assertEquals(ComplaintStatus.SUBMITTED, saved.getStatus());
        verify(auditLogService).recordComplaintAudit(
            eq(14L),
            eq("PUBLIC"),
            eq("公众用户"),
            eq("COMPLAINT_SUBMIT"),
            eq("提交投诉"),
            eq(null),
            any(Complaint.class),
            eq("公众用户提交投诉，当前状态为待受理")
        );
    }

    @Test
    void submitPublic_shouldSnapshotPublicUserForNonAnonymousComplaint() {
        ComplaintMapper complaintMapper = mock(ComplaintMapper.class);
        ComplaintDataSupport complaintDataSupport = mock(ComplaintDataSupport.class);
        ComplaintLockSupport complaintLockSupport = mock(ComplaintLockSupport.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        ComplaintAuditOperatorNameResolver resolver = new ComplaintAuditOperatorNameResolver();
        ComplaintCommandService service = new ComplaintCommandService(
            complaintMapper,
            complaintDataSupport,
            complaintLockSupport,
            auditLogService,
            resolver
        );

        InternalEnterpriseDetailVO enterprise = new InternalEnterpriseDetailVO();
        enterprise.setId(5L);
        UserVO submitter = new UserVO();
        submitter.setRealName("李四");
        submitter.setPhone("13900001234");
        AtomicReference<Complaint> insertedComplaint = new AtomicReference<>();

        when(complaintDataSupport.requireEnterprise(5L)).thenReturn(enterprise);
        when(complaintDataSupport.requirePublicUserById(15L)).thenReturn(submitter);
        when(complaintDataSupport.generateComplaintNo()).thenReturn("CPT202605060002");
        when(complaintDataSupport.trim("李四")).thenReturn("李四");
        when(complaintDataSupport.trim("13900001234")).thenReturn("13900001234");
        when(complaintDataSupport.trim(null)).thenReturn(null);
        when(complaintDataSupport.serializeImageUrls(null)).thenReturn(null);
        doAnswer(invocation -> {
            Complaint complaint = invocation.getArgument(0);
            insertedComplaint.set(complaint);
            complaint.setId(101L);
            return 1;
        }).when(complaintMapper).insert(any(Complaint.class));

        ComplaintSubmitDTO dto = new ComplaintSubmitDTO();
        dto.setEnterpriseId(5L);
        dto.setAnonymous(false);
        dto.setContent("门店卫生情况较差");

        service.submitPublic(15L, dto);

        Complaint saved = insertedComplaint.get();
        assertNotNull(saved);
        assertEquals("李四", saved.getComplainantName());
        assertEquals("13900001234", saved.getContact());
        assertEquals(0, saved.getAnonymousFlag());
        assertEquals(ComplaintStatus.SUBMITTED, saved.getStatus());
        verify(complaintDataSupport, never()).normalizeComplaintType(any());
    }

    @Test
    void assign_shouldApplyDefaultDeadlineWhenMissing() {
        ComplaintMapper complaintMapper = mock(ComplaintMapper.class);
        ComplaintDataSupport complaintDataSupport = mock(ComplaintDataSupport.class);
        ComplaintLockSupport complaintLockSupport = mock(ComplaintLockSupport.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        ComplaintAuditOperatorNameResolver resolver = new ComplaintAuditOperatorNameResolver();
        ComplaintCommandService service = new ComplaintCommandService(
            complaintMapper,
            complaintDataSupport,
            complaintLockSupport,
            auditLogService,
            resolver
        );

        InternalRegulatorIdentityVO admin = new InternalRegulatorIdentityVO();
        admin.setId(10L);
        admin.setUserId(1L);
        admin.setRoleType(ComplaintDataSupport.ROLE_ADMIN);
        admin.setName("张区域");
        admin.setUsername("zhangqy");

        Complaint complaint = new Complaint();
        complaint.setId(100L);
        complaint.setStatus(ComplaintStatus.PENDING);

        InternalRegulatorSummaryVO enforcer = new InternalRegulatorSummaryVO();
        enforcer.setId(20L);
        enforcer.setName("李执法");
        enforcer.setRoleType(ComplaintDataSupport.ROLE_ENFORCER);

        ComplaintVO result = new ComplaintVO();

        when(complaintDataSupport.requireRegulatorByUserId(1L)).thenReturn(admin);
        when(complaintDataSupport.requireComplaint(100L)).thenReturn(complaint);
        when(complaintDataSupport.requireRegulatorById(9L, "assignee not found")).thenReturn(enforcer);
        when(complaintDataSupport.toVOWithNames(complaint)).thenReturn(result);
        when(complaintLockSupport.executeWithLock(any(), any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(2, Callable.class).call());
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
}
