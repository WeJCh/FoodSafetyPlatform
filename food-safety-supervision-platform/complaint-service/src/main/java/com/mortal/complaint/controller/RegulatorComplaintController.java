package com.mortal.complaint.controller;

import com.mortal.complaint.application.ComplaintCommandService;
import com.mortal.complaint.application.ComplaintQueryService;
import com.mortal.complaint.dto.ComplaintAssignDTO;
import com.mortal.complaint.dto.ComplaintHandleDTO;
import com.mortal.complaint.dto.ComplaintRejectDTO;
import com.mortal.complaint.support.RequestIdentityResolver;
import com.mortal.complaint.support.RequestIdentityResolver.RequestIdentity;
import com.mortal.complaint.vo.AuditLogVO;
import com.mortal.complaint.vo.ComplaintDetailVO;
import com.mortal.complaint.vo.ComplaintVO;
import com.mortal.platform.common.ApiResponse;
import com.mortal.platform.common.PageResult;
import jakarta.validation.Valid;
import java.util.List;
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

    public RegulatorComplaintController(ComplaintCommandService complaintCommandService,
                                        ComplaintQueryService complaintQueryService,
                                        RequestIdentityResolver requestIdentityResolver) {
        this.complaintCommandService = complaintCommandService;
        this.complaintQueryService = complaintQueryService;
        this.requestIdentityResolver = requestIdentityResolver;
    }

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

    @GetMapping("/{id}/logs")
    public ApiResponse<List<AuditLogVO>> logs(@RequestHeader(value = "X-User-Id", required = false)
                                              String userId,
                                              @RequestHeader(value = "X-User-Type", required = false)
                                              String userType,
                                              @RequestHeader(value = "X-User-Roles", required = false)
                                              String userRoles,
                                              @PathVariable Long id,
                                              @RequestParam(defaultValue = "10") int limit) {
        RequestIdentity identity = requestIdentityResolver.resolve(userId, userType, userRoles);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(complaintQueryService.listLogs(identity.userId(), id, limit));
    }

    @GetMapping("/logs/recent")
    public ApiResponse<List<AuditLogVO>> recentLogs(@RequestHeader(value = "X-User-Id", required = false)
                                                    String userId,
                                                    @RequestHeader(value = "X-User-Type", required = false)
                                                    String userType,
                                                    @RequestHeader(value = "X-User-Roles", required = false)
                                                    String userRoles,
                                                    @RequestParam(defaultValue = "10") int limit) {
        RequestIdentity identity = requestIdentityResolver.resolve(userId, userType, userRoles);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(complaintQueryService.listRecentLogs(identity.userId(), limit));
    }

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
