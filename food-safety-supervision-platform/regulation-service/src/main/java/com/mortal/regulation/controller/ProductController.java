package com.mortal.regulation.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.dto.ProductSaveDTO;
import com.mortal.regulation.service.ProductService;
import com.mortal.regulation.util.JwtUserResolver;
import com.mortal.regulation.vo.ProductVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    private final ProductService productService;
    private final JwtUserResolver jwtUserResolver;

    public ProductController(ProductService productService,
                             JwtUserResolver jwtUserResolver) {
        this.productService = productService;
        this.jwtUserResolver = jwtUserResolver;
    }
    /**
     * 获取当前用户的产品列表
     * 
     * @param token 令牌
     * @return 产品列表
     */
    @GetMapping("/api/regulation/products/my")
    public ApiResponse<List<ProductVO>> listMyProducts(@RequestHeader("Authorization") String token) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isEnterprise()) {
            return ApiResponse.failure(403, "enterprise user only");
        }
        return ApiResponse.success(productService.listMyProducts(identity.userId()));
    }

    /**
     * 创建产品
     * 
     * @param token 令牌
     * @param dto 产品保存DTO
     * @return 产品VO
     */
    @PostMapping("/api/regulation/products")
    public ApiResponse<ProductVO> createMyProduct(@RequestHeader("Authorization") String token,
                                                  @Valid @RequestBody ProductSaveDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isEnterprise()) {
            return ApiResponse.failure(403, "enterprise user only");
        }
        return ApiResponse.success(productService.createMyProduct(identity.userId(), dto));
    }

    /**
     * 更新产品
     * 
     * @param token 令牌
     * @param id 产品ID
     * @param dto 产品保存DTO
     * @return 产品VO
     */
    @PutMapping("/api/regulation/products/{id}")
    public ApiResponse<ProductVO> updateMyProduct(@RequestHeader("Authorization") String token,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody ProductSaveDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isEnterprise()) {
            return ApiResponse.failure(403, "enterprise user only");
        }
        return ApiResponse.success(productService.updateMyProduct(identity.userId(), id, dto));
    }

    /**
     * 获取企业的产品列表
     * 
     * @param token 令牌
     * @param enterpriseId 企业ID
     * @return 产品列表
     */
    @GetMapping("/api/regulation/enterprises/{enterpriseId}/products")
    public ApiResponse<List<ProductVO>> listEnterpriseProducts(@RequestHeader("Authorization") String token,
                                                               @PathVariable Long enterpriseId) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(productService.listByEnterpriseIdForRegulator(identity.userId(), enterpriseId));
    }

    private UserIdentity resolveIdentity(String token) {
        Long userId = jwtUserResolver.resolveUserId(token);
        String userType = jwtUserResolver.resolveUserType(token);
        if (userId == null) {
            throw new IllegalArgumentException("unauthorized");
        }
        return new UserIdentity(userId, userType);
    }

    private record UserIdentity(Long userId, String userType) {

        boolean isEnterprise() {
            return "ENTERPRISE".equals(userType);
        }

        boolean isRegulator() {
            return "REGULATOR".equals(userType) || "ADMIN".equals(userType);
        }
    }
}
