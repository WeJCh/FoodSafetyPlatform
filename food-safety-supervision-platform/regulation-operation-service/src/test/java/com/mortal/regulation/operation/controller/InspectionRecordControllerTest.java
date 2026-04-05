package com.mortal.regulation.operation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mortal.platform.common.ApiResponse;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.operation.common.RequestIdentity;
import com.mortal.regulation.operation.service.InspectionRecordService;
import com.mortal.regulation.operation.support.RequestIdentityResolver;
import com.mortal.regulation.operation.vo.InspectionRecordDetailVO;
import com.mortal.regulation.operation.vo.InspectionRecordVO;
import java.util.List;
import org.junit.jupiter.api.Test;

class InspectionRecordControllerTest {

    @Test
    void listEnterprise_shouldRejectNonEnterpriseUser() {
        InspectionRecordService service = mock(InspectionRecordService.class);
        RequestIdentityResolver resolver = mock(RequestIdentityResolver.class);
        InspectionRecordController controller = new InspectionRecordController(service, resolver);

        when(resolver.resolve("Bearer test")).thenReturn(new RequestIdentity(101L, "REGULATOR"));

        ApiResponse<PageResult<InspectionRecordVO>> response =
            controller.listEnterprise("Bearer test", null, null, null, 1, 10);

        assertEquals(403, response.getCode());
        assertEquals("enterprise only", response.getMessage());
    }

    @Test
    void listEnterprise_shouldReturnOwnRecords() {
        InspectionRecordService service = mock(InspectionRecordService.class);
        RequestIdentityResolver resolver = mock(RequestIdentityResolver.class);
        InspectionRecordController controller = new InspectionRecordController(service, resolver);
        InspectionRecordVO record = new InspectionRecordVO();
        record.setId(1L);
        record.setEnterpriseName("示例企业");
        PageResult<InspectionRecordVO> page = PageResult.of(List.of(record), 1, 1, 10);

        when(resolver.resolve("Bearer test")).thenReturn(new RequestIdentity(201L, "ENTERPRISE"));
        when(service.listForEnterprise(201L, "FAIL", null, null, 1, 10)).thenReturn(page);

        ApiResponse<PageResult<InspectionRecordVO>> response =
            controller.listEnterprise("Bearer test", "FAIL", null, null, 1, 10);

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertEquals("示例企业", response.getData().getRecords().get(0).getEnterpriseName());
        verify(service).listForEnterprise(201L, "FAIL", null, null, 1, 10);
    }

    @Test
    void detailEnterprise_shouldTranslateMissingRecordTo404() {
        InspectionRecordService service = mock(InspectionRecordService.class);
        RequestIdentityResolver resolver = mock(RequestIdentityResolver.class);
        InspectionRecordController controller = new InspectionRecordController(service, resolver);

        when(resolver.resolve("Bearer test")).thenReturn(new RequestIdentity(201L, "ENTERPRISE"));
        when(service.getDetailForEnterprise(201L, 404L)).thenThrow(new IllegalArgumentException("record not found"));

        ApiResponse<InspectionRecordDetailVO> response = controller.detailEnterprise("Bearer test", 404L);

        assertEquals(404, response.getCode());
        assertEquals("record not found", response.getMessage());
        verify(service).getDetailForEnterprise(201L, 404L);
    }
}
