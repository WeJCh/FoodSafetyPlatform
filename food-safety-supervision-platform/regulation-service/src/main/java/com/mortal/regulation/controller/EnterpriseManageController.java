package com.mortal.regulation.controller;

import com.mortal.regulation.common.ApiResponse;
import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.service.EnterpriseProfileService;
import com.mortal.regulation.util.JwtUserResolver;
import com.mortal.regulation.vo.EnterpriseProfileVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/regulation/enterprises")
public class EnterpriseManageController {

    private final EnterpriseProfileService enterpriseProfileService;
    private final JwtUserResolver jwtUserResolver;

    public EnterpriseManageController(EnterpriseProfileService enterpriseProfileService,
                                      JwtUserResolver jwtUserResolver) {
        this.enterpriseProfileService = enterpriseProfileService;
        this.jwtUserResolver = jwtUserResolver;
    }

    @GetMapping
    public ApiResponse<PageResult<EnterpriseProfileVO>> list(@RequestHeader("Authorization") String token,
                                                             @RequestParam(required = false) String enterpriseName,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(required = false) String approvalStatus,
                                                             @RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(enterpriseProfileService.list(enterpriseName, status, approvalStatus, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<EnterpriseProfileVO> detail(@RequestHeader("Authorization") String token,
                                                   @PathVariable Long id) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        EnterpriseProfileVO enterprise = enterpriseProfileService.getById(id);
        if (enterprise == null) {
            return ApiResponse.failure(404, "enterprise not found");
        }
        return ApiResponse.success(enterprise);
    }

    @GetMapping("/me")
    public ApiResponse<EnterpriseProfileVO> me(@RequestHeader("Authorization") String token) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isEnterprise()) {
            return ApiResponse.failure(403, "enterprise user only");
        }
        EnterpriseProfileVO enterprise = enterpriseProfileService.getProfile(identity.userId());
        if (enterprise == null) {
            return ApiResponse.failure(404, "enterprise not found");
        }
        return ApiResponse.success(enterprise);
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
