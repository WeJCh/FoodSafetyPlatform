package com.mortal.regulation.controller;

import com.mortal.regulation.common.ApiResponse;
import com.mortal.regulation.common.PageResult;
import com.mortal.regulation.dto.InspectionSubmitDTO;
import com.mortal.regulation.dto.InspectionTaskAssignDTO;
import com.mortal.regulation.dto.InspectionTaskCreateDTO;
import com.mortal.regulation.service.InspectionTaskService;
import com.mortal.regulation.util.JwtUserResolver;
import com.mortal.regulation.vo.InspectionTaskVO;
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

@RestController
@RequestMapping("/api/regulation/tasks")
public class InspectionTaskController {

    private final InspectionTaskService inspectionTaskService;
    private final JwtUserResolver jwtUserResolver;

    public InspectionTaskController(InspectionTaskService inspectionTaskService, JwtUserResolver jwtUserResolver) {
        this.inspectionTaskService = inspectionTaskService;
        this.jwtUserResolver = jwtUserResolver;
    }

    @PostMapping
    public ApiResponse<InspectionTaskVO> create(@RequestHeader("Authorization") String token,
                                                @Valid @RequestBody InspectionTaskCreateDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(inspectionTaskService.createTask(identity.userId(), dto));
    }

    @PutMapping("/{id}/assign")
    public ApiResponse<InspectionTaskVO> assign(@RequestHeader("Authorization") String token,
                                                @PathVariable Long id,
                                                @Valid @RequestBody InspectionTaskAssignDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(inspectionTaskService.assignTask(identity.userId(), id, dto));
    }

    @GetMapping
    public ApiResponse<PageResult<InspectionTaskVO>> list(@RequestHeader("Authorization") String token,
                                                          @RequestParam(required = false) String enterpriseName,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(
            inspectionTaskService.listTasksForAdmin(identity.userId(), enterpriseName, status, page, size)
        );
    }

    @GetMapping("/my")
    public ApiResponse<PageResult<InspectionTaskVO>> listMy(@RequestHeader("Authorization") String token,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(
            inspectionTaskService.listTasksForEnforcer(identity.userId(), status, page, size)
        );
    }

    @PutMapping("/{id}/start")
    public ApiResponse<InspectionTaskVO> start(@RequestHeader("Authorization") String token,
                                               @PathVariable Long id) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(inspectionTaskService.startTask(identity.userId(), id));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<InspectionTaskVO> submit(@RequestHeader("Authorization") String token,
                                                @PathVariable Long id,
                                                @Valid @RequestBody InspectionSubmitDTO dto) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(inspectionTaskService.submitTask(identity.userId(), id, dto));
    }

    @PutMapping("/{id}/close")
    public ApiResponse<InspectionTaskVO> close(@RequestHeader("Authorization") String token,
                                               @PathVariable Long id) {
        UserIdentity identity = resolveIdentity(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, "regulator only");
        }
        return ApiResponse.success(inspectionTaskService.closeTask(identity.userId(), id));
    }

    private UserIdentity resolveIdentity(String token) {
        Long userId = jwtUserResolver.resolveUserId(token);
        String userType = jwtUserResolver.resolveUserType(token);
        if (userId == null) {
            throw new IllegalArgumentException("unauthorized");
        }
        return new UserIdentity(userId, userType);
    }

    private record UserIdentity(Long userId, String userType) {

        boolean isRegulator() {
            return "REGULATOR".equals(userType) || "ADMIN".equals(userType);
        }
    }
}
