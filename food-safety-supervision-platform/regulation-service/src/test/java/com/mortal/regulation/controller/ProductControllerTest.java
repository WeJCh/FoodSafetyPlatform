package com.mortal.regulation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.dto.ProductSaveDTO;
import com.mortal.regulation.service.ProductService;
import com.mortal.regulation.util.JwtUserResolver;
import com.mortal.regulation.vo.ProductVO;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductControllerTest {

    @Test
    void listMyProducts_shouldReturnEnterpriseProducts() {
        ProductService productService = mock(ProductService.class);
        JwtUserResolver jwtUserResolver = mock(JwtUserResolver.class);
        ProductController controller = new ProductController(productService, jwtUserResolver);
        ProductVO product = new ProductVO();
        product.setId(11L);
        product.setProductName("鲜牛奶");

        when(jwtUserResolver.resolveUserId("Bearer token")).thenReturn(8L);
        when(jwtUserResolver.resolveUserType("Bearer token")).thenReturn("ENTERPRISE");
        when(productService.listMyProducts(8L)).thenReturn(List.of(product));

        ApiResponse<List<ProductVO>> response = controller.listMyProducts("Bearer token");

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertEquals("鲜牛奶", response.getData().get(0).getProductName());
        verify(productService).listMyProducts(8L);
    }

    @Test
    void createMyProduct_shouldRejectNonEnterpriseUser() {
        ProductService productService = mock(ProductService.class);
        JwtUserResolver jwtUserResolver = mock(JwtUserResolver.class);
        ProductController controller = new ProductController(productService, jwtUserResolver);

        when(jwtUserResolver.resolveUserId("Bearer token")).thenReturn(1L);
        when(jwtUserResolver.resolveUserType("Bearer token")).thenReturn("ADMIN");

        ApiResponse<ProductVO> response = controller.createMyProduct("Bearer token", new ProductSaveDTO());

        assertEquals(403, response.getCode());
        assertEquals("enterprise user only", response.getMessage());
    }

    @Test
    void listEnterpriseProducts_shouldReturnRegulatorView() {
        ProductService productService = mock(ProductService.class);
        JwtUserResolver jwtUserResolver = mock(JwtUserResolver.class);
        ProductController controller = new ProductController(productService, jwtUserResolver);
        ProductVO product = new ProductVO();
        product.setId(7L);
        product.setProductName("冷冻水饺");

        when(jwtUserResolver.resolveUserId("Bearer token")).thenReturn(3L);
        when(jwtUserResolver.resolveUserType("Bearer token")).thenReturn("REGULATOR");
        when(productService.listByEnterpriseIdForRegulator(3L, 100L)).thenReturn(List.of(product));

        ApiResponse<List<ProductVO>> response = controller.listEnterpriseProducts("Bearer token", 100L);

        assertEquals(0, response.getCode());
        assertEquals("冷冻水饺", response.getData().get(0).getProductName());
        verify(productService).listByEnterpriseIdForRegulator(3L, 100L);
    }
}
