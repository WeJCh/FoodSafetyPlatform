package com.mortal.regulation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mortal.platform.common.ApiResponse;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.service.BulletinService;
import com.mortal.regulation.vo.BulletinDetailVO;
import com.mortal.regulation.vo.BulletinVO;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublicBulletinControllerTest {

    @Test
    void list_shouldReturnPublishedBulletins() {
        BulletinService service = mock(BulletinService.class);
        PublicBulletinController controller = new PublicBulletinController(service);
        BulletinVO record = new BulletinVO();
        record.setId(11L);
        record.setTitle("食品安全提示");
        PageResult<BulletinVO> page = PageResult.of(List.of(record), 1, 1, 10);

        when(service.listPublic("提示", 1, 10)).thenReturn(page);

        ApiResponse<PageResult<BulletinVO>> response = controller.list("提示", 1, 10);

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertEquals("食品安全提示", response.getData().getRecords().get(0).getTitle());
        verify(service).listPublic("提示", 1, 10);
    }

    @Test
    void detail_shouldReturnPublishedBulletin() {
        BulletinService service = mock(BulletinService.class);
        PublicBulletinController controller = new PublicBulletinController(service);
        BulletinDetailVO detail = new BulletinDetailVO();
        detail.setId(1001L);
        detail.setTitle("节假日食品安全提醒");

        when(service.getPublicDetail(1001L)).thenReturn(detail);

        ApiResponse<BulletinDetailVO> response = controller.detail(1001L);

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertEquals("节假日食品安全提醒", response.getData().getTitle());
        verify(service).getPublicDetail(1001L);
    }

    @Test
    void detail_shouldReturnNotFoundWhenInvisible() {
        BulletinService service = mock(BulletinService.class);
        PublicBulletinController controller = new PublicBulletinController(service);

        when(service.getPublicDetail(404L)).thenReturn(null);

        ApiResponse<BulletinDetailVO> response = controller.detail(404L);

        assertEquals(404, response.getCode());
        assertEquals("public bulletin not found", response.getMessage());
        verify(service).getPublicDetail(404L);
    }
}
