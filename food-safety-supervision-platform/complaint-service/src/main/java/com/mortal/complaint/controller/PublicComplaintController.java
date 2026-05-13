package com.mortal.complaint.controller;

import com.mortal.complaint.application.ComplaintCommandService;
import com.mortal.complaint.application.ComplaintQueryService;
import com.mortal.platform.common.ApiResponse;
import com.mortal.platform.common.PageResult;
import com.mortal.complaint.dto.ComplaintSubmitDTO;
import com.mortal.complaint.support.ComplaintRateLimitService;
import com.mortal.complaint.support.RequestIdentityResolver;
import com.mortal.complaint.support.RequestIdentityResolver.RequestIdentity;
import com.mortal.complaint.vo.ComplaintListVO;
import com.mortal.complaint.vo.ComplaintPublicStatsVO;
import com.mortal.complaint.vo.ComplaintTrackVO;
import com.mortal.complaint.vo.ComplaintVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共投诉控制器
 */
@RestController
@RequestMapping("/api/complaints")
public class PublicComplaintController {

    private final ComplaintCommandService complaintCommandService;
    private final ComplaintQueryService complaintQueryService;
    private final RequestIdentityResolver requestIdentityResolver;
    private final ComplaintRateLimitService complaintRateLimitService;

    /**
     * 构造函数
     * @param complaintCommandService 投诉命令服务
     * @param complaintQueryService 投诉查询服务
     * @param requestIdentityResolver 请求身份解析器
     */
    public PublicComplaintController(ComplaintCommandService complaintCommandService,
                                     ComplaintQueryService complaintQueryService,
                                     RequestIdentityResolver requestIdentityResolver,
                                     ComplaintRateLimitService complaintRateLimitService) {
        this.complaintCommandService = complaintCommandService;
        this.complaintQueryService = complaintQueryService;
        this.requestIdentityResolver = requestIdentityResolver;
        this.complaintRateLimitService = complaintRateLimitService;
    }

    /**
     * 提交公共投诉
     * @param userId 用户ID
     * @param userType 用户类型
     * @param userRoles 用户角色
     * @param dto 投诉提交DTO
     * @return 投诉跟踪VO
     */
    @PostMapping("/public")
    public ApiResponse<ComplaintTrackVO> submitPublic(@RequestHeader(value = "X-User-Id", required = false)
                                                      String userId,
                                                      @RequestHeader(value = "X-User-Type", required = false)
                                                      String userType,
                                                      @RequestHeader(value = "X-User-Roles", required = false)
                                                      String userRoles,
                                                      @Valid @RequestBody ComplaintSubmitDTO dto) {
        RequestIdentity identity = requestIdentityResolver.resolve(userId, userType, userRoles);
        if (!identity.isPublicUser()) {
            return ApiResponse.failure(403, "public user only");
        }
        if (!complaintRateLimitService.isPublicSubmitAllowed(identity.userId())) {
            return ApiResponse.failure(429, "complaint submit requests are too frequent");
        }
        return ApiResponse.success(complaintCommandService.submitPublic(identity.userId(), dto));
    }

    /**
     * 查询我的公共投诉
     * @param userId 用户ID
     * @param userType 用户类型
     * @param userRoles 用户角色
     * @param status 状态
     * @param page 页码
     * @param size 每页大小
     * @return 投诉列表
     */
    @GetMapping("/my")
    public ApiResponse<PageResult<ComplaintListVO>> my(@RequestHeader(value = "X-User-Id", required = false)
                                                       String userId,
                                                       @RequestHeader(value = "X-User-Type", required = false)
                                                       String userType,
                                                       @RequestHeader(value = "X-User-Roles", required = false)
                                                       String userRoles,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        RequestIdentity identity = requestIdentityResolver.resolve(userId, userType, userRoles);
        if (!identity.isPublicUser()) {
            return ApiResponse.failure(403, "public user only");
        }
        return ApiResponse.success(complaintQueryService.listMyPublic(identity.userId(), status, keyword, page, size));
    }

    @GetMapping("/my/stats")
    public ApiResponse<ComplaintPublicStatsVO> myStats(@RequestHeader(value = "X-User-Id", required = false)
                                                       String userId,
                                                       @RequestHeader(value = "X-User-Type", required = false)
                                                       String userType,
                                                       @RequestHeader(value = "X-User-Roles", required = false)
                                                       String userRoles,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) String keyword) {
        RequestIdentity identity = requestIdentityResolver.resolve(userId, userType, userRoles);
        if (!identity.isPublicUser()) {
            return ApiResponse.failure(403, "public user only");
        }
        return ApiResponse.success(complaintQueryService.statsMyPublic(identity.userId(), status, keyword));
    }

    /**
     * 获取我的公共投诉详情
     * @param userId 用户ID
     * @param userType 用户类型
     * @param userRoles 用户角色
     * @param id 投诉ID
     * @return 投诉详情
     */
    @GetMapping("/my/{id}")
    public ApiResponse<ComplaintVO> myDetail(@RequestHeader(value = "X-User-Id", required = false)
                                             String userId,
                                             @RequestHeader(value = "X-User-Type", required = false)
                                             String userType,
                                             @RequestHeader(value = "X-User-Roles", required = false)
                                             String userRoles,
                                             @PathVariable Long id) {
        RequestIdentity identity = requestIdentityResolver.resolve(userId, userType, userRoles);
        if (!identity.isPublicUser()) {
            return ApiResponse.failure(403, "public user only");
        }
        return ApiResponse.success(complaintQueryService.getMyPublicDetail(identity.userId(), id));
    }
}

