package com.mortal.complaint.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortal.complaint.domain.entity.AuditLog;
import com.mortal.complaint.domain.entity.Complaint;
import com.mortal.complaint.infrastructure.mapper.AuditLogMapper;
import com.mortal.complaint.vo.AuditLogVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AuditLogServiceTest {

    @Test
    void listRecentComplaintLogs_shouldKeepOperatorName() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        AuditLogService service = new AuditLogService(auditLogMapper, new ObjectMapper());

        AuditLog publicLog = new AuditLog();
        publicLog.setId(1L);
        publicLog.setTargetType("COMPLAINT");
        publicLog.setTargetId(100L);
        publicLog.setActionType("COMPLAINT_SUBMIT");
        publicLog.setOperatorName("公众用户");
        publicLog.setAfterData("{\"status\":\"SUBMITTED\"}");
        publicLog.setCreateTime(LocalDateTime.now());

        AuditLog regulatorLog = new AuditLog();
        regulatorLog.setId(2L);
        regulatorLog.setTargetType("COMPLAINT");
        regulatorLog.setTargetId(101L);
        regulatorLog.setActionType("COMPLAINT_ASSIGN");
        regulatorLog.setOperatorName("张区域（zhangqy）");
        regulatorLog.setBeforeData("{\"status\":\"PENDING\"}");
        regulatorLog.setAfterData("{\"status\":\"ASSIGNED\"}");
        regulatorLog.setCreateTime(LocalDateTime.now());

        when(auditLogMapper.selectList(any())).thenReturn(List.of(regulatorLog, publicLog));

        List<AuditLogVO> result = service.listRecentComplaintLogs(List.of(100L, 101L), 10);

        assertEquals(2, result.size());
        assertEquals("张区域（zhangqy）", result.get(0).getOperatorName());
        assertEquals("公众用户", result.get(1).getOperatorName());
    }

    @Test
    void recordComplaintAudit_shouldUseSystemFallbackWhenOperatorNameBlank() {
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        AuditLogService service = new AuditLogService(auditLogMapper, new ObjectMapper());
        AtomicReference<AuditLog> saved = new AtomicReference<>();

        doAnswer(invocation -> {
            saved.set(invocation.getArgument(0));
            return 1;
        }).when(auditLogMapper).insert(any(AuditLog.class));

        Complaint complaint = new Complaint();
        complaint.setId(1L);
        complaint.setComplaintNo("COMP202605060001");
        complaint.setSubmitterUserId(14L);

        service.recordComplaintAudit(
            1L,
            "REGULATOR_ADMIN",
            "  ",
            "COMPLAINT_ACCEPT",
            "受理投诉",
            null,
            complaint,
            "投诉已受理，状态由待受理调整为待分派"
        );

        assertNotNull(saved.get());
        assertEquals("系统", saved.get().getOperatorName());
    }
}
