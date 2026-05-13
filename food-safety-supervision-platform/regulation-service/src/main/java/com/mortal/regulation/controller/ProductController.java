package com.mortal.regulation.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.dto.ProductSaveDTO;
import com.mortal.regulation.service.ProductService;
import com.mortal.regulation.util.JwtUserResolver;
import com.mortal.regulation.vo.AuditLogVO;
import com.mortal.regulation.vo.ProductVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/api/regulation/products/my")
    public ApiResponse<List<ProductVO>> listMyProducts(@RequestHeader("Authorization") String token) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isEnterprise()) {
            return ApiResponse.failure(403, "仅企业账号可访问");
        }
        return ApiResponse.success(productService.listMyProducts(identity.userId()));
    }

    @PostMapping("/api/regulation/products")
    public ApiResponse<ProductVO> createMyProduct(@RequestHeader("Authorization") String token,
                                                  @Valid @RequestBody ProductSaveDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isEnterprise()) {
            return ApiResponse.failure(403, "仅企业账号可访问");
        }
        return ApiResponse.success(productService.createMyProduct(identity.userId(), identity.username(), dto));
    }

    @PutMapping("/api/regulation/products/{id}")
    public ApiResponse<ProductVO> updateMyProduct(@RequestHeader("Authorization") String token,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody ProductSaveDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isEnterprise()) {
            return ApiResponse.failure(403, "仅企业账号可访问");
        }
        return ApiResponse.success(productService.updateMyProduct(identity.userId(), identity.username(), id, dto));
    }

    @GetMapping("/api/regulation/products/{id}/logs")
    public ApiResponse<List<AuditLogVO>> listMyProductLogs(@RequestHeader("Authorization") String token,
                                                           @PathVariable Long id,
                                                           @RequestParam(required = false) Integer limit) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isEnterprise()) {
            return ApiResponse.failure(403, "仅企业账号可访问");
        }
        return ApiResponse.success(productService.listMyProductLogs(identity.userId(), id, limit));
    }

    @GetMapping("/api/regulation/enterprises/{enterpriseId}/products")
    public ApiResponse<List<ProductVO>> listEnterpriseProducts(@RequestHeader("Authorization") String token,
                                                               @PathVariable Long enterpriseId) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "仅监管账号可访问");
        }
        try {
            return ApiResponse.success(productService.listByEnterpriseIdForRegulator(identity.userId(), enterpriseId));
        } catch (IllegalArgumentException ex) {
            if ("unauthorized".equals(ex.getMessage())) {
                return ApiResponse.failure(403, "无权查看该企业产品档案");
            }
            throw ex;
        }
    }

    @GetMapping("/api/regulation/enterprises/{enterpriseId}/products/{id}/logs")
    public ApiResponse<List<AuditLogVO>> listEnterpriseProductLogs(@RequestHeader("Authorization") String token,
                                                                   @PathVariable Long enterpriseId,
                                                                   @PathVariable Long id,
                                                                   @RequestParam(required = false) Integer limit) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "仅监管账号可访问");
        }
        try {
            return ApiResponse.success(
                productService.listProductLogsForRegulator(identity.userId(), enterpriseId, id, limit)
            );
        } catch (IllegalArgumentException ex) {
            if ("unauthorized".equals(ex.getMessage())) {
                return ApiResponse.failure(403, "无权查看该企业产品日志");
            }
            throw ex;
        }
    }

    private UserIdentity resolveIdentity(String token) {
        Long userId = jwtUserResolver.resolveUserId(token);
        String userType = jwtUserResolver.resolveUserType(token);
        String username = jwtUserResolver.resolveUsername(token);
        if (userId == null) {
            throw new IllegalArgumentException("unauthorized");
        }
        return new UserIdentity(userId, userType, username);
    }

    private record UserIdentity(Long userId, String userType, String username) {

        boolean isEnterprise() {
            return "ENTERPRISE".equals(userType);
        }

        boolean isRegulator() {
            return "REGULATOR".equals(userType) || "ADMIN".equals(userType);
        }
    }
}
