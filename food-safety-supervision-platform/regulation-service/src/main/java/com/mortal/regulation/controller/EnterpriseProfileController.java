package com.mortal.regulation.controller;

import com.mortal.regulation.common.ApiResponse;
import com.mortal.regulation.dto.EnterpriseApprovalDTO;
import com.mortal.regulation.dto.EnterpriseProfileDTO;
import com.mortal.regulation.service.EnterpriseProfileService;
import com.mortal.regulation.util.JwtUserResolver;
import com.mortal.regulation.vo.EnterpriseProfileVO;
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
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/regulation/enterprise")
public class EnterpriseProfileController {

    private final EnterpriseProfileService enterpriseProfileService;
    private final JwtUserResolver jwtUserResolver;

    public EnterpriseProfileController(EnterpriseProfileService enterpriseProfileService,
                                       JwtUserResolver jwtUserResolver) {
        this.enterpriseProfileService = enterpriseProfileService;
        this.jwtUserResolver = jwtUserResolver;
    }

    @PostMapping("/profile")
    public ApiResponse<EnterpriseProfileVO> submitProfile(@RequestHeader("Authorization") String token,
                                                          @Valid @RequestBody EnterpriseProfileDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isEnterprise()) {
            return ApiResponse.failure(403, "enterprise user only");
        }
        return ApiResponse.success(enterpriseProfileService.submitProfile(identity.userId(), dto));
    }

    @GetMapping("/profile")
    public ApiResponse<EnterpriseProfileVO> getProfile(@RequestHeader("Authorization") String token) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isEnterprise()) {
            return ApiResponse.failure(403, "enterprise user only");
        }
        EnterpriseProfileVO profile = enterpriseProfileService.getProfile(identity.userId());
        if (profile == null) {
            return ApiResponse.failure(404, "enterprise profile not found");
        }
        return ApiResponse.success(profile);
    }

    @GetMapping("/pending")
    public ApiResponse<List<EnterpriseProfileVO>> listPending(@RequestHeader("Authorization") String token) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(enterpriseProfileService.listPending());
    }

    @PutMapping("/{id}/approve")
    public ApiResponse<EnterpriseProfileVO> approve(@RequestHeader("Authorization") String token,
                                                    @PathVariable Long id,
                                                    @RequestBody(required = false) EnterpriseApprovalDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(enterpriseProfileService.approve(id, identity.userId(), dto));
    }

    @PutMapping("/{id}/reject")
    public ApiResponse<EnterpriseProfileVO> reject(@RequestHeader("Authorization") String token,
                                                   @PathVariable Long id,
                                                   @RequestBody(required = false) EnterpriseApprovalDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(enterpriseProfileService.reject(id, identity.userId(), dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteEnterprise(@RequestHeader("Authorization") String token,
                                              @PathVariable Long id) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        enterpriseProfileService.deleteEnterprise(id);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/profile")
    public ApiResponse<Void> deleteMyEnterprise(@RequestHeader("Authorization") String token) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isEnterprise()) {
            return ApiResponse.failure(403, "enterprise user only");
        }
        enterpriseProfileService.deleteEnterpriseByUserId(identity.userId());
        return ApiResponse.success(null);
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
