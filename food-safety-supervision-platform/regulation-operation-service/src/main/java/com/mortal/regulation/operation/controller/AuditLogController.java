package com.mortal.regulation.operation.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.operation.common.RequestIdentity;
import com.mortal.regulation.operation.service.AuditLogQueryService;
import com.mortal.regulation.operation.support.RequestIdentityResolver;
import com.mortal.regulation.operation.vo.AuditLogVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/regulation-operation/audit-logs")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;
    private final RequestIdentityResolver requestIdentityResolver;

    public AuditLogController(AuditLogQueryService auditLogQueryService,
                              RequestIdentityResolver requestIdentityResolver) {
        this.auditLogQueryService = auditLogQueryService;
        this.requestIdentityResolver = requestIdentityResolver;
    }

    @GetMapping("/{targetType}/{targetId}")
    public ApiResponse<List<AuditLogVO>> listTargetLogs(@RequestHeader("Authorization") String token,
                                                        @PathVariable String targetType,
                                                        @PathVariable Long targetId,
                                                        @RequestParam(defaultValue = "10") int limit) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        return ApiResponse.success(
            auditLogQueryService.listLogs(identity.userId(), identity.userType(), targetType, targetId, limit)
        );
    }

    @GetMapping("/recent")
    public ApiResponse<List<AuditLogVO>> listRecentLogs(@RequestHeader("Authorization") String token,
                                                        @RequestParam(required = false) String bizType,
                                                        @RequestParam(defaultValue = "10") int limit) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        return ApiResponse.success(
            auditLogQueryService.listRecentLogs(identity.userId(), identity.userType(), bizType, limit)
        );
    }
}
