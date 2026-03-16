package com.mortal.complaint.controller;

import com.mortal.complaint.application.ComplaintCommandService;
import com.mortal.complaint.application.ComplaintQueryService;
import com.mortal.platform.common.ApiResponse;
import com.mortal.platform.common.PageResult;
import com.mortal.complaint.dto.ComplaintAssignDTO;
import com.mortal.complaint.dto.ComplaintHandleDTO;
import com.mortal.complaint.dto.ComplaintRejectDTO;
import com.mortal.complaint.support.RequestIdentityResolver;
import com.mortal.complaint.support.RequestIdentityResolver.RequestIdentity;
import com.mortal.complaint.vo.ComplaintDetailVO;
import com.mortal.complaint.vo.ComplaintVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 监管投诉控制器
 */
@RestController
@RequestMapping("/api/complaints")
public class RegulatorComplaintController {

    private final ComplaintCommandService complaintCommandService;
    private final ComplaintQueryService complaintQueryService;
    private final RequestIdentityResolver requestIdentityResolver;

    /**
     * 构造函数
     * @param complaintCommandService 投诉命令服务
     * @param complaintQueryService 投诉查询服务
     * @param requestIdentityResolver 请求身份解析器
     */
    public RegulatorComplaintController(ComplaintCommandService complaintCommandService,
                                        ComplaintQueryService complaintQueryService,
                                        RequestIdentityResolver requestIdentityResolver) {
        this.complaintCommandService = complaintCommandService;
        this.complaintQueryService = complaintQueryService;
        this.requestIdentityResolver = requestIdentityResolver;
    }

    /**
     * 查询投诉列表
     * @param userId 用户ID
     * @param userType 用户类型
     * @param userRoles 用户角色
     * @param status 状态
     * @param enterpriseName 企业名称
     * @param assignedToName 分配给名称
     * @param assignedByName 分配给用户名称
     * @param page 页码
     * @param size 每页大小
     * @return 投诉列表
     */
    @GetMapping
    public ApiResponse<PageResult<ComplaintVO>> list(@RequestHeader(value = "X-User-Id", required = false)
                                                     String userId,
                                                     @RequestHeader(value = "X-User-Type", required = false)
                                                     String userType,
                                                     @RequestHeader(value = "X-User-Roles", required = false)
                                                     String userRoles,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String enterpriseName,
                                                     @RequestParam(required = false) String assignedToName,
                                                     @RequestParam(required = false) String assignedByName,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        RequestIdentity identity = requestIdentityResolver.resolve(userId, userType, userRoles);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(complaintQueryService.list(
            identity.userId(), status, enterpriseName, assignedToName, assignedByName, page, size));
    }

    /**
     * 获取投诉详情
     * @param userId 用户ID
     * @param userType 用户类型
     * @param userRoles 用户角色
     * @param id 投诉ID
     * @return 投诉详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ComplaintDetailVO> detail(@RequestHeader(value = "X-User-Id", required = false)
                                                 String userId,
                                                 @RequestHeader(value = "X-User-Type", required = false)
                                                 String userType,
                                                 @RequestHeader(value = "X-User-Roles", required = false)
                                                 String userRoles,
                                                 @PathVariable Long id) {
        RequestIdentity identity = requestIdentityResolver.resolve(userId, userType, userRoles);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(complaintQueryService.getDetail(identity.userId(), id));
    }

    /**
     * 接受投诉
     * @param userId 用户ID
     * @param userType 用户类型
     * @param userRoles 用户角色
     * @param id 投诉ID
     * @return 投诉VO
     */
    @PutMapping("/{id}/accept")
    public ApiResponse<ComplaintVO> accept(@RequestHeader(value = "X-User-Id", required = false)
                                           String userId,
                                           @RequestHeader(value = "X-User-Type", required = false)
                                           String userType,
                                           @RequestHeader(value = "X-User-Roles", required = false)
                                           String userRoles,
                                           @PathVariable Long id) {
        RequestIdentity identity = requestIdentityResolver.resolve(userId, userType, userRoles);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(complaintCommandService.accept(identity.userId(), id));
    }

    /**
     * 分配投诉
     * @param userId 用户ID
     * @param userType 用户类型
     * @param userRoles 用户角色
     * @param id 投诉ID
     * @param dto 投诉分配DTO
     * @return 投诉VO
     */
    @PutMapping("/{id}/assign")
    public ApiResponse<ComplaintVO> assign(@RequestHeader(value = "X-User-Id", required = false)
                                           String userId,
                                           @RequestHeader(value = "X-User-Type", required = false)
                                           String userType,
                                           @RequestHeader(value = "X-User-Roles", required = false)
                                           String userRoles,
                                           @PathVariable Long id,
                                           @Valid @RequestBody ComplaintAssignDTO dto) {
        RequestIdentity identity = requestIdentityResolver.resolve(userId, userType, userRoles);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(complaintCommandService.assign(identity.userId(), id, dto));
    }

    /**
     * 开始处理投诉
     * @param userId 用户ID
     * @param userType 用户类型
     * @param userRoles 用户角色
     * @param id 投诉ID
     * @return 投诉VO
     */
    @PutMapping("/{id}/process")
    public ApiResponse<ComplaintVO> startProcess(@RequestHeader(value = "X-User-Id", required = false)
                                                 String userId,
                                                 @RequestHeader(value = "X-User-Type", required = false)
                                                 String userType,
                                                 @RequestHeader(value = "X-User-Roles", required = false)
                                                 String userRoles,
                                                 @PathVariable Long id) {
        RequestIdentity identity = requestIdentityResolver.resolve(userId, userType, userRoles);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(complaintCommandService.startProcess(identity.userId(), id));
    }

    /**
     * 处理投诉
     * @param userId 用户ID
     * @param userType 用户类型
     * @param userRoles 用户角色
     * @param id 投诉ID
     * @param dto 投诉处理DTO
     * @return 投诉VO
     */
    @PostMapping("/{id}/handle")
    public ApiResponse<ComplaintVO> handle(@RequestHeader(value = "X-User-Id", required = false)
                                           String userId,
                                           @RequestHeader(value = "X-User-Type", required = false)
                                           String userType,
                                           @RequestHeader(value = "X-User-Roles", required = false)
                                           String userRoles,
                                           @PathVariable Long id,
                                           @Valid @RequestBody ComplaintHandleDTO dto) {
        RequestIdentity identity = requestIdentityResolver.resolve(userId, userType, userRoles);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(complaintCommandService.handle(identity.userId(), id, dto));
    }

    /**
     * 驳回投诉
     * @param userId 用户ID
     * @param userType 用户类型
     * @param userRoles 用户角色
     * @param id 投诉ID
     * @param dto 投诉驳回DTO
     * @return 投诉VO
     */
    @PutMapping("/{id}/reject")
    public ApiResponse<ComplaintVO> reject(@RequestHeader(value = "X-User-Id", required = false)
                                           String userId,
                                           @RequestHeader(value = "X-User-Type", required = false)
                                           String userType,
                                           @RequestHeader(value = "X-User-Roles", required = false)
                                           String userRoles,
                                           @PathVariable Long id,
                                           @Valid @RequestBody ComplaintRejectDTO dto) {
        RequestIdentity identity = requestIdentityResolver.resolve(userId, userType, userRoles);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(complaintCommandService.reject(identity.userId(), id, dto));
    }
}

