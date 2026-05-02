package com.mortal.regulation.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.dto.RegulatorProfileDTO;
import com.mortal.regulation.dto.RegulatorRegionAdjustDTO;
import com.mortal.regulation.dto.RegulatorStatusDTO;
import com.mortal.regulation.service.RegulatorProfileService;
import com.mortal.regulation.util.JwtUserResolver;
import com.mortal.regulation.vo.AuditLogVO;
import com.mortal.regulation.vo.RegulatorProfileVO;
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
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/regulation/regulators")
public class RegulatorProfileController {

    private final RegulatorProfileService regulatorProfileService;
    private final JwtUserResolver jwtUserResolver;

    public RegulatorProfileController(RegulatorProfileService regulatorProfileService,
                                      JwtUserResolver jwtUserResolver) {
        this.regulatorProfileService = regulatorProfileService;
        this.jwtUserResolver = jwtUserResolver;
    }

    @PostMapping
    public ApiResponse<RegulatorProfileVO> createOrUpdate(@RequestHeader("Authorization") String token,
                                                          @Valid @RequestBody RegulatorProfileDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isAdmin()) {
            return ApiResponse.failure(403, "admin only");
        }
        return ApiResponse.success(regulatorProfileService.createOrUpdate(identity.userId(), identity.username(), dto));
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<RegulatorProfileVO> getByUserId(@RequestHeader("Authorization") String token,
                                                       @PathVariable Long userId) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isAdmin()) {
            return ApiResponse.failure(403, "admin only");
        }
        RegulatorProfileVO profile = regulatorProfileService.getByUserId(userId);
        if (profile == null) {
            return ApiResponse.failure(404, "regulator not found");
        }
        return ApiResponse.success(profile);
    }

    @GetMapping("/me")
    public ApiResponse<RegulatorProfileVO> getMyProfile(@RequestHeader("Authorization") String token) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        RegulatorProfileVO profile = regulatorProfileService.getByUserId(identity.userId());
        if (profile == null) {
            return ApiResponse.failure(404, "regulator not found");
        }
        return ApiResponse.success(profile);
    }

    @GetMapping
    public ApiResponse<List<RegulatorProfileVO>> list(@RequestHeader("Authorization") String token,
                                                      @RequestParam(required = false) String roleType,
                                                      @RequestParam(required = false) Long regionId) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isAdmin()) {
            return ApiResponse.failure(403, "admin only");
        }
        return ApiResponse.success(regulatorProfileService.list(roleType, regionId));
    }

    @GetMapping("/eligible")
    public ApiResponse<List<RegulatorProfileVO>> listEligible(@RequestHeader("Authorization") String token,
                                                              @RequestParam(required = false) Long regionId) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        RegulatorProfileVO profile = regulatorProfileService.getByUserId(identity.userId());
        if (profile == null) {
            return ApiResponse.failure(404, "regulator not found");
        }
        if (!"REGULATOR_ADMIN".equals(profile.getRoleType())) {
            return ApiResponse.failure(403, "regulator admin only");
        }
        return ApiResponse.success(regulatorProfileService.listEligibleEnforcers(identity.userId(), regionId));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<RegulatorProfileVO> updateStatus(@RequestHeader("Authorization") String token,
                                                        @PathVariable Long id,
                                                        @Valid @RequestBody RegulatorStatusDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isAdmin()) {
            return ApiResponse.failure(403, "admin only");
        }
        return ApiResponse.success(regulatorProfileService.updateStatus(identity.userId(), identity.username(), id, dto.getStatus()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRegulator(@RequestHeader("Authorization") String token,
                                             @PathVariable Long id) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isAdmin()) {
            return ApiResponse.failure(403, "admin only");
        }
        regulatorProfileService.deleteRegulator(identity.userId(), identity.username(), id);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/regions")
    public ApiResponse<RegulatorProfileVO> adjustRegions(@RequestHeader("Authorization") String token,
                                                         @PathVariable Long id,
                                                         @Valid @RequestBody RegulatorRegionAdjustDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isAdmin()) {
            return ApiResponse.failure(403, "admin only");
        }
        return ApiResponse.success(
            regulatorProfileService.adjustRegions(identity.userId(), identity.username(), id, dto.getRegionIds(), dto.getRemark())
        );
    }

    @GetMapping("/{id}/audit-logs")
    public ApiResponse<List<AuditLogVO>> listAuditLogs(@RequestHeader("Authorization") String token,
                                                       @PathVariable Long id,
                                                       @RequestParam(required = false) Integer limit) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isAdmin()) {
            return ApiResponse.failure(403, "admin only");
        }
        return ApiResponse.success(regulatorProfileService.listAuditLogs(id, limit));
    }

    @GetMapping("/audit-logs/recent")
    public ApiResponse<List<AuditLogVO>> listRecentAuditLogs(@RequestHeader("Authorization") String token,
                                                             @RequestParam(required = false) Integer limit) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isAdmin()) {
            return ApiResponse.failure(403, "admin only");
        }
        return ApiResponse.success(regulatorProfileService.listRecentAuditLogs(limit));
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

        boolean isAdmin() {
            return "ADMIN".equals(userType);
        }

        boolean isRegulator() {
            return "REGULATOR".equals(userType);
        }
    }
}

