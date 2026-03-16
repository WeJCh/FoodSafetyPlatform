package com.mortal.regulation.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.dto.WarningActionCommentDTO;
import com.mortal.regulation.dto.WarningAssignDTO;
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
     * 区域管理员指派预警。
     */
    @PostMapping("/{id}/assign")
    public ApiResponse<WarningRecordDetailVO> adminAssign(@RequestHeader("Authorization") String token,
                                                          @PathVariable("id") Long id,
                                                          @Valid @RequestBody WarningAssignDTO assignDTO) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        try {
            return ApiResponse.success(
                warningProxyService.assignAdminWarning(identity.userId(), identity.username(), id, assignDTO)
            );
        } catch (IllegalArgumentException ex) {
            if ("admin only".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(403, "admin only");
            }
            throw ex;
        }
    }

    /**
     * 区域管理员转处理中。
     */
    @PostMapping("/{id}/process")
    public ApiResponse<WarningRecordDetailVO> adminProcess(@RequestHeader("Authorization") String token,
                                                           @PathVariable("id") Long id,
                                                           @RequestBody(required = false) WarningActionCommentDTO actionDTO) {
        return processAdminAction(token, id, "PROCESS", actionDTO);
    }

    /**
     * 区域管理员标记已解决。
     */
    @PostMapping("/{id}/resolve")
    public ApiResponse<WarningRecordDetailVO> adminResolve(@RequestHeader("Authorization") String token,
                                                           @PathVariable("id") Long id,
                                                           @RequestBody(required = false) WarningActionCommentDTO actionDTO) {
        return processAdminAction(token, id, "RESOLVE", actionDTO);
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
     * 执法员转处理中。
     */
    @PostMapping("/my/{id}/process")
    public ApiResponse<WarningRecordDetailVO> myProcess(@RequestHeader("Authorization") String token,
                                                        @PathVariable("id") Long id,
                                                        @RequestBody(required = false) WarningActionCommentDTO actionDTO) {
        return processMyAction(token, id, "PROCESS", actionDTO);
    }

    /**
     * 执法员标记已解决。
     */
    @PostMapping("/my/{id}/resolve")
    public ApiResponse<WarningRecordDetailVO> myResolve(@RequestHeader("Authorization") String token,
                                                        @PathVariable("id") Long id,
                                                        @RequestBody(required = false) WarningActionCommentDTO actionDTO) {
        return processMyAction(token, id, "RESOLVE", actionDTO);
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

    private WarningProcessActionDTO buildAction(String actionType, WarningActionCommentDTO source) {
        WarningProcessActionDTO dto = new WarningProcessActionDTO();
        dto.setActionType(actionType);
        if (source != null) {
            dto.setActionComment(source.getActionComment());
        }
        return dto;
    }

    private ApiResponse<WarningRecordDetailVO> processAdminAction(String token,
                                                                  Long id,
                                                                  String actionType,
                                                                  WarningActionCommentDTO actionDTO) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        try {
            return ApiResponse.success(
                warningProxyService.processAdminWarning(
                    identity.userId(),
                    identity.username(),
                    id,
                    buildAction(actionType, actionDTO)
                )
            );
        } catch (IllegalArgumentException ex) {
            if ("admin only".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(403, "admin only");
            }
            throw ex;
        }
    }

    private ApiResponse<WarningRecordDetailVO> processMyAction(String token,
                                                               Long id,
                                                               String actionType,
                                                               WarningActionCommentDTO actionDTO) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        try {
            return ApiResponse.success(
                warningProxyService.processMyWarning(
                    identity.userId(),
                    identity.username(),
                    id,
                    buildAction(actionType, actionDTO)
                )
            );
        } catch (IllegalArgumentException ex) {
            if ("enforcer only".equalsIgnoreCase(ex.getMessage())) {
                return ApiResponse.failure(403, "enforcer only");
            }
            throw ex;
        }
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

