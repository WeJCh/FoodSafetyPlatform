package com.mortal.regulation.controller;

import com.mortal.regulation.common.ApiResponse;
import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.dto.WarningProcessActionDTO;
import com.mortal.regulation.dto.WarningRecordQueryDTO;
import com.mortal.regulation.service.WarningProxyService;
import com.mortal.regulation.util.JwtUserResolver;
import com.mortal.regulation.vo.WarningRecordDetailVO;
import com.mortal.regulation.vo.WarningRecordVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 监管侧预警代理接口（管理员与执法员分权限访问）。
 */
@RestController
@RequestMapping("/api/regulation/warnings")
public class WarningProxyController {

    private final WarningProxyService warningProxyService;
    private final JwtUserResolver jwtUserResolver;

    public WarningProxyController(WarningProxyService warningProxyService,
                                  JwtUserResolver jwtUserResolver) {
        this.warningProxyService = warningProxyService;
        this.jwtUserResolver = jwtUserResolver;
    }

    /**
     * 查询当前区域管理员可见预警列表。
     */
    @GetMapping
    public ApiResponse<PageResult<WarningRecordVO>> adminList(@RequestHeader("Authorization") String token,
                                                              WarningRecordQueryDTO queryDTO) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        try {
            return ApiResponse.success(warningProxyService.listAdminWarnings(identity.userId(), queryDTO));
        } catch (IllegalArgumentException ex) {
            if ("admin only".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(403, "admin only");
            }
            throw ex;
        }
    }

    /**
     * 查询当前区域管理员可见预警详情。
     */
    @GetMapping("/{id}")
    public ApiResponse<WarningRecordDetailVO> adminDetail(@RequestHeader("Authorization") String token,
                                                          @PathVariable("id") Long id) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        try {
            return ApiResponse.success(warningProxyService.getAdminWarningDetail(identity.userId(), id));
        } catch (IllegalArgumentException ex) {
            if ("admin only".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(403, "admin only");
            }
            throw ex;
        }
    }

    /**
     * 区域管理员处理预警动作。
     */
    @PostMapping("/{id}/actions")
    public ApiResponse<WarningRecordDetailVO> adminAction(@RequestHeader("Authorization") String token,
                                                          @PathVariable("id") Long id,
                                                          @Valid @RequestBody WarningProcessActionDTO actionDTO) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        try {
            return ApiResponse.success(
                warningProxyService.processAdminWarning(identity.userId(), identity.username(), id, actionDTO)
            );
        } catch (IllegalArgumentException ex) {
            if ("admin only".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(403, "admin only");
            }
            throw ex;
        }
    }

    /**
     * 查询当前执法员可见预警列表。
     */
    @GetMapping("/my")
    public ApiResponse<PageResult<WarningRecordVO>> myList(@RequestHeader("Authorization") String token,
                                                           WarningRecordQueryDTO queryDTO) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        try {
            return ApiResponse.success(warningProxyService.listMyWarnings(identity.userId(), queryDTO));
        } catch (IllegalArgumentException ex) {
            if ("enforcer only".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(403, "enforcer only");
            }
            throw ex;
        }
    }

    /**
     * 查询当前执法员可见预警详情。
     */
    @GetMapping("/my/{id}")
    public ApiResponse<WarningRecordDetailVO> myDetail(@RequestHeader("Authorization") String token,
                                                       @PathVariable("id") Long id) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        try {
            return ApiResponse.success(warningProxyService.getMyWarningDetail(identity.userId(), id));
        } catch (IllegalArgumentException ex) {
            if ("enforcer only".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(403, "enforcer only");
            }
            throw ex;
        }
    }

    /**
     * 执法员处理预警动作。
     */
    @PostMapping("/my/{id}/actions")
    public ApiResponse<WarningRecordDetailVO> myAction(@RequestHeader("Authorization") String token,
                                                       @PathVariable("id") Long id,
                                                       @Valid @RequestBody WarningProcessActionDTO actionDTO) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        try {
            return ApiResponse.success(
                warningProxyService.processMyWarning(identity.userId(), identity.username(), id, actionDTO)
            );
        } catch (IllegalArgumentException ex) {
            if ("enforcer only".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(403, "enforcer only");
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
            if ("ADMIN".equalsIgnoreCase(userType) || "REGULATOR".equalsIgnoreCase(userType)) {
                return true;
            }
            return userType != null && userType.toUpperCase().startsWith("REGULATOR_");
        }
    }
}
