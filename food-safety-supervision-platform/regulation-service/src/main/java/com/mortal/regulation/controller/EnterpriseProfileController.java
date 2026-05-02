package com.mortal.regulation.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.dto.EnterpriseApprovalBatchDTO;
import com.mortal.regulation.dto.EnterpriseApprovalDTO;
import com.mortal.regulation.dto.EnterpriseProfileDTO;
import com.mortal.regulation.service.EnterpriseProfileService;
import com.mortal.regulation.util.JwtUserResolver;
import com.mortal.regulation.vo.BatchActionResult;
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

/**
 * 企业Profile控制器
 */
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

    /**
     * 提交企业Profile
     * @param token 令牌
     * @param dto 企业ProfileDTO
     * @return 企业ProfileVO
     */
    @PostMapping("/profile")
    public ApiResponse<EnterpriseProfileVO> submitProfile(@RequestHeader("Authorization") String token,
                                                          @Valid @RequestBody EnterpriseProfileDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isEnterprise()) {
            return ApiResponse.failure(403, "enterprise user only");
        }
        return ApiResponse.success(
            enterpriseProfileService.submitProfile(identity.userId(), identity.userType(), identity.username(), dto)
        );
    }

    /**
     * 获取企业Profile
     * @param token 令牌
     * @return 企业ProfileVO
     */
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

    /**
     * 获取待审核企业列表
     * @param token 令牌
     * @return 企业ProfileVO列表
     */
    @GetMapping("/pending")
    public ApiResponse<List<EnterpriseProfileVO>> listPending(@RequestHeader("Authorization") String token) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        if (identity.isAdmin()) {
            return ApiResponse.success(enterpriseProfileService.listPending());
        }
        return ApiResponse.success(enterpriseProfileService.listPendingForRegulator(identity.userId()));
    }

    /**
     * 审核企业
     * @param token 令牌
     * @param id 企业ID
     * @param dto 企业审核DTO
     * @return 企业ProfileVO
     */
    @PutMapping("/{id}/approve")
    public ApiResponse<EnterpriseProfileVO> approve(@RequestHeader("Authorization") String token,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody EnterpriseApprovalDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(
            enterpriseProfileService.approve(id, identity.userId(), identity.userType(), identity.username(), dto)
        );
    }

    /**
     * 驳回企业
     * @param token 令牌
     * @param id 企业ID
     * @param dto 企业驳回DTO
     * @return 企业ProfileVO
     */
    @PutMapping("/{id}/reject")
    public ApiResponse<EnterpriseProfileVO> reject(@RequestHeader("Authorization") String token,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody EnterpriseApprovalDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(
            enterpriseProfileService.reject(id, identity.userId(), identity.userType(), identity.username(), dto)
        );
    }

    /**
     * 批量审核企业
     * @param token 令牌
     * @param dto 企业审核DTO
     * @return 批量审核结果
     */
    @PutMapping("/approve-batch")
    public ApiResponse<BatchActionResult> approveBatch(@RequestHeader("Authorization") String token,
                                                       @Valid @RequestBody EnterpriseApprovalBatchDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(
            enterpriseProfileService.approveBatch(identity.userId(), identity.userType(), identity.username(), dto)
        );
    }

    /**
     * 批量驳回企业
     * @param token 令牌
     * @param dto 企业驳回DTO
     * @return 批量驳回结果
     */
    @PutMapping("/reject-batch")
    public ApiResponse<BatchActionResult> rejectBatch(@RequestHeader("Authorization") String token,
                                                      @Valid @RequestBody EnterpriseApprovalBatchDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(
            enterpriseProfileService.rejectBatch(identity.userId(), identity.userType(), identity.username(), dto)
        );
    }

    /**
     * 删除企业
     * @param token 令牌
     * @param id 企业ID
     * @return 空
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteEnterprise(@RequestHeader("Authorization") String token,
                                              @PathVariable Long id) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        enterpriseProfileService.deleteEnterprise(id, identity.userId(), identity.userType(), identity.username());
        return ApiResponse.success(null);
    }

    /**
     * 删除当前企业
     * @param token 令牌
     * @return 空
     */
    @DeleteMapping("/profile")
    public ApiResponse<Void> deleteMyEnterprise(@RequestHeader("Authorization") String token) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isEnterprise()) {
            return ApiResponse.failure(403, "enterprise user only");
        }
        enterpriseProfileService.deleteEnterpriseByUserId(identity.userId());
        return ApiResponse.success(null);
    }

    /**
     * 解析用户身份
     * @param token 令牌
     * @return 用户身份
     */
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

        boolean isAdmin() {
            return "ADMIN".equals(userType);
        }
    }
}

