package com.mortal.regulation.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.dto.BulletinSaveDTO;
import com.mortal.regulation.service.AuditLogService;
import com.mortal.regulation.service.BulletinService;
import com.mortal.regulation.util.JwtUserResolver;
import com.mortal.regulation.vo.AuditLogVO;
import com.mortal.regulation.vo.BulletinDetailVO;
import com.mortal.regulation.vo.BulletinVO;
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

@RestController
@RequestMapping("/api/regulation/bulletins")
public class BulletinManageController {

    private final BulletinService bulletinService;
    private final AuditLogService auditLogService;
    private final JwtUserResolver jwtUserResolver;

    public BulletinManageController(BulletinService bulletinService,
                                    AuditLogService auditLogService,
                                    JwtUserResolver jwtUserResolver) {
        this.bulletinService = bulletinService;
        this.auditLogService = auditLogService;
        this.jwtUserResolver = jwtUserResolver;
    }

    @GetMapping
    public ApiResponse<PageResult<BulletinVO>> list(@RequestHeader("Authorization") String token,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String category,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        try {
            return ApiResponse.success(bulletinService.listAdmin(identity.userId(), keyword, category, status, page, size));
        } catch (IllegalArgumentException ex) {
            if ("admin only".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(403, "admin only");
            }
            throw ex;
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<BulletinDetailVO> detail(@RequestHeader("Authorization") String token,
                                                @PathVariable Long id) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        try {
            return ApiResponse.success(bulletinService.getAdminDetail(identity.userId(), id));
        } catch (IllegalArgumentException ex) {
            if ("admin only".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(403, "admin only");
            }
            if ("bulletin not found".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(404, "bulletin not found");
            }
            throw ex;
        }
    }

    @GetMapping("/{id}/logs")
    public ApiResponse<List<AuditLogVO>> listAuditLogs(@RequestHeader("Authorization") String token,
                                                       @PathVariable Long id,
                                                       @RequestParam(required = false) Integer limit) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        try {
            bulletinService.getAdminDetail(identity.userId(), id);
            return ApiResponse.success(auditLogService.listBulletinLogs(id, limit == null ? 8 : limit));
        } catch (IllegalArgumentException ex) {
            if ("admin only".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(403, "admin only");
            }
            if ("bulletin not found".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(404, "bulletin not found");
            }
            throw ex;
        }
    }

    @GetMapping("/logs/recent")
    public ApiResponse<List<AuditLogVO>> listRecentAuditLogs(@RequestHeader("Authorization") String token,
                                                             @RequestParam(required = false) Integer limit) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        try {
            bulletinService.listAdmin(identity.userId(), null, null, null, 1, 1);
            return ApiResponse.success(auditLogService.listRecentBulletinLogs(limit == null ? 10 : limit));
        } catch (IllegalArgumentException ex) {
            if ("admin only".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(403, "admin only");
            }
            throw ex;
        }
    }

    @PostMapping
    public ApiResponse<BulletinDetailVO> create(@RequestHeader("Authorization") String token,
                                                @Valid @RequestBody BulletinSaveDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        try {
            return ApiResponse.success(bulletinService.create(identity.userId(), identity.username(), dto));
        } catch (IllegalArgumentException ex) {
            if ("admin only".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(403, "admin only");
            }
            throw ex;
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<BulletinDetailVO> update(@RequestHeader("Authorization") String token,
                                                @PathVariable Long id,
                                                @Valid @RequestBody BulletinSaveDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        try {
            return ApiResponse.success(bulletinService.update(identity.userId(), identity.username(), id, dto));
        } catch (IllegalArgumentException ex) {
            if ("admin only".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(403, "admin only");
            }
            if ("bulletin not found".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(404, "bulletin not found");
            }
            throw ex;
        }
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<BulletinDetailVO> publish(@RequestHeader("Authorization") String token,
                                                 @PathVariable Long id) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        try {
            return ApiResponse.success(bulletinService.publish(identity.userId(), identity.username(), id));
        } catch (IllegalArgumentException ex) {
            if ("admin only".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(403, "admin only");
            }
            if ("bulletin not found".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(404, "bulletin not found");
            }
            throw ex;
        }
    }

    @PostMapping("/{id}/offline")
    public ApiResponse<BulletinDetailVO> offline(@RequestHeader("Authorization") String token,
                                                 @PathVariable Long id) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        try {
            return ApiResponse.success(bulletinService.offline(identity.userId(), identity.username(), id));
        } catch (IllegalArgumentException ex) {
            if ("admin only".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(403, "admin only");
            }
            if ("bulletin not found".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(404, "bulletin not found");
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

        boolean isRegulator() {
            return "REGULATOR".equalsIgnoreCase(userType);
        }
    }
}
