package com.mortal.regulation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.controller.internal.InternalProductController;
import com.mortal.regulation.service.ProductService;
import com.mortal.regulation.support.ProductMasterCacheService;
import com.mortal.regulation.vo.internal.InternalProductDetailVO;
import com.mortal.regulation.vo.internal.InternalProductSummaryVO;
import java.util.List;
import org.junit.jupiter.api.Test;

class InternalProductControllerTest {

    @Test
    void getById_shouldReturnInternalProductDetail() {
        ProductService productService = mock(ProductService.class);
        ProductMasterCacheService productMasterCacheService = mock(ProductMasterCacheService.class);
        InternalProductController controller = new InternalProductController(productService, productMasterCacheService);
        InternalProductDetailVO detail = new InternalProductDetailVO();
        detail.setId(9L);
        detail.setProductName("方便面");

        when(productMasterCacheService.getDetail(eq(9L), any())).thenReturn(detail);

        ApiResponse<InternalProductDetailVO> response = controller.getById(9L);

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertEquals("方便面", response.getData().getProductName());
        verify(productMasterCacheService).getDetail(eq(9L), any());
    }

    @Test
    void listByEnterprise_shouldReturnProductSummaries() {
        ProductService productService = mock(ProductService.class);
        ProductMasterCacheService productMasterCacheService = mock(ProductMasterCacheService.class);
        InternalProductController controller = new InternalProductController(productService, productMasterCacheService);
        InternalProductSummaryVO summary = new InternalProductSummaryVO();
        summary.setId(5L);
        summary.setProductName("酸奶");

        when(productService.listInternalByEnterpriseId(12L)).thenReturn(List.of(summary));

        ApiResponse<List<InternalProductSummaryVO>> response = controller.listByEnterprise(12L);

        assertEquals(0, response.getCode());
        assertEquals("酸奶", response.getData().get(0).getProductName());
        verify(productService).listInternalByEnterpriseId(12L);
    }

    @Test
    void queryIdsByName_shouldDelegateToProductService() {
        ProductService productService = mock(ProductService.class);
        ProductMasterCacheService productMasterCacheService = mock(ProductMasterCacheService.class);
        InternalProductController controller = new InternalProductController(productService, productMasterCacheService);
        when(productService.queryInternalProductIdsByNameKeyword("奶")).thenReturn(List.of(101L, 102L));

        ApiResponse<List<Long>> response = controller.queryIdsByName("奶");

        assertEquals(0, response.getCode());
        assertEquals(List.of(101L, 102L), response.getData());
        verify(productService).queryInternalProductIdsByNameKeyword("奶");
    }
}
