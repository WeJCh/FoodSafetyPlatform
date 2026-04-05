package com.mortal.regulation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mortal.platform.common.ApiResponse;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.service.EnterpriseProfileService;
import com.mortal.regulation.vo.PublicEnterpriseDetailVO;
import com.mortal.regulation.vo.PublicEnterpriseVO;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublicEnterpriseControllerTest {

    @Test
    void list_shouldReturnPublicEnterprisePage() {
        EnterpriseProfileService service = mock(EnterpriseProfileService.class);
        PublicEnterpriseController controller = new PublicEnterpriseController(service);
        PublicEnterpriseVO record = new PublicEnterpriseVO();
        record.setId(101L);
        record.setEnterpriseName("示例企业");
        PageResult<PublicEnterpriseVO> page = PageResult.of(List.of(record), 1, 1, 10);

        when(service.listPublic("示例", 1, 10)).thenReturn(page);

        ApiResponse<PageResult<PublicEnterpriseVO>> response = controller.list("示例", 1, 10);

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().getTotal());
        assertEquals("示例企业", response.getData().getRecords().get(0).getEnterpriseName());
        verify(service).listPublic("示例", 1, 10);
    }

    @Test
    void detail_shouldReturnPublicEnterpriseDetail() {
        EnterpriseProfileService service = mock(EnterpriseProfileService.class);
        PublicEnterpriseController controller = new PublicEnterpriseController(service);
        PublicEnterpriseDetailVO detail = new PublicEnterpriseDetailVO();
        detail.setId(1001L);
        detail.setEnterpriseName("公开企业");

        when(service.getPublicById(1001L)).thenReturn(detail);

        ApiResponse<PublicEnterpriseDetailVO> response = controller.detail(1001L);

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertEquals("公开企业", response.getData().getEnterpriseName());
        verify(service).getPublicById(1001L);
    }

    @Test
    void detail_shouldReturnNotFoundWhenEnterpriseInvisible() {
        EnterpriseProfileService service = mock(EnterpriseProfileService.class);
        PublicEnterpriseController controller = new PublicEnterpriseController(service);

        when(service.getPublicById(404L)).thenReturn(null);

        ApiResponse<PublicEnterpriseDetailVO> response = controller.detail(404L);

        assertEquals(404, response.getCode());
        assertEquals("public enterprise not found", response.getMessage());
        verify(service).getPublicById(404L);
    }
}
