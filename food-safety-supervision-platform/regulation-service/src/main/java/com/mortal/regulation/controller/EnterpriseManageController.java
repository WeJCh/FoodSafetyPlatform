package com.mortal.regulation.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.service.EnterpriseProfileService;
import com.mortal.regulation.util.JwtUserResolver;
import com.mortal.regulation.vo.AuditLogVO;
import com.mortal.regulation.vo.EnterpriseProfileVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业管理控制器
 */
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

    /**
     * 获取企业列表
     * @param token 令牌
     * @param enterpriseName 企业名称
     * @param status 企业状态
     * @param approvalStatus 审核状态
     * @param page 页码
     * @param size 每页大小
     * @return 企业列表
     */
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
        if (identity.isAdmin()) {
            return ApiResponse.success(enterpriseProfileService.list(enterpriseName, status, approvalStatus, page, size));
        }
        return ApiResponse.success(
            enterpriseProfileService.listForRegulator(identity.userId(), enterpriseName, status, approvalStatus, page, size)
        );
    }

    /**
     * 获取企业详情
     * @param token 令牌
     * @param id 企业ID
     * @return 企业详情
     */
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

    @GetMapping("/{id}/audit-logs")
    public ApiResponse<List<AuditLogVO>> listAuditLogs(@RequestHeader("Authorization") String token,
                                                       @PathVariable Long id,
                                                       @RequestParam(required = false) Integer limit) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(enterpriseProfileService.listAuditLogs(id, limit));
    }

    @GetMapping("/audit-logs/recent")
    public ApiResponse<List<AuditLogVO>> listRecentAuditLogs(@RequestHeader("Authorization") String token,
                                                             @RequestParam(required = false) Integer limit) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(enterpriseProfileService.listRecentAuditLogs(limit));
    }

    /**
     * 获取当前企业信息
     * @param token 令牌
     * @return 企业信息
     */
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

    /**
     * 解析用户身份
     * @param token 令牌
     * @return 用户身份
     */
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

        boolean isAdmin() {
            return "ADMIN".equals(userType);
        }
    }
}

