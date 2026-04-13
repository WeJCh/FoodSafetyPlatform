package com.mortal.regulation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mortal.platform.common.ApiResponse;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.service.BulletinService;
import com.mortal.regulation.util.JwtUserResolver;
import com.mortal.regulation.vo.BulletinVO;
import java.util.List;
import org.junit.jupiter.api.Test;

class BulletinManageControllerTest {

    @Test
    void list_shouldReturnForbiddenWhenUserIsNotRegulator() {
        BulletinService service = mock(BulletinService.class);
        JwtUserResolver resolver = mock(JwtUserResolver.class);
        BulletinManageController controller = new BulletinManageController(service, resolver);

        when(resolver.resolveUserId("Bearer test")).thenReturn(100L);
        when(resolver.resolveUserType("Bearer test")).thenReturn("PUBLIC");

        ApiResponse<PageResult<BulletinVO>> response = controller.list("Bearer test", null, null, null, 1, 10);

        assertEquals(403, response.getCode());
        assertEquals("regulator only", response.getMessage());
    }

    @Test
    void list_shouldTranslateAdminOnlyToForbidden() {
        BulletinService service = mock(BulletinService.class);
        JwtUserResolver resolver = mock(JwtUserResolver.class);
        BulletinManageController controller = new BulletinManageController(service, resolver);

        when(resolver.resolveUserId("Bearer test")).thenReturn(100L);
        when(resolver.resolveUserType("Bearer test")).thenReturn("REGULATOR");
        when(service.listAdmin(100L, "公告", "NOTICE", "DRAFT", 1, 10))
            .thenThrow(new IllegalArgumentException("admin only"));

        ApiResponse<PageResult<BulletinVO>> response = controller.list("Bearer test", "公告", "NOTICE", "DRAFT", 1, 10);

        assertEquals(403, response.getCode());
        assertEquals("admin only", response.getMessage());
        verify(service).listAdmin(100L, "公告", "NOTICE", "DRAFT", 1, 10);
    }

    @Test
    void list_shouldReturnPageWhenAuthorized() {
        BulletinService service = mock(BulletinService.class);
        JwtUserResolver resolver = mock(JwtUserResolver.class);
        BulletinManageController controller = new BulletinManageController(service, resolver);
        BulletinVO record = new BulletinVO();
        record.setId(1L);
        record.setTitle("监管公告");
        PageResult<BulletinVO> page = PageResult.of(List.of(record), 1, 1, 10);

        when(resolver.resolveUserId("Bearer test")).thenReturn(100L);
        when(resolver.resolveUserType("Bearer test")).thenReturn("REGULATOR");
        when(service.listAdmin(100L, null, null, null, 1, 10)).thenReturn(page);

        ApiResponse<PageResult<BulletinVO>> response = controller.list("Bearer test", null, null, null, 1, 10);

        assertEquals(0, response.getCode());
        assertEquals("监管公告", response.getData().getRecords().get(0).getTitle());
        verify(service).listAdmin(100L, null, null, null, 1, 10);
    }
}