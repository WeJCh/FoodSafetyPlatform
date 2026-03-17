package com.mortal.regulation.operation.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.operation.common.OperationErrorMessages;
import com.mortal.regulation.operation.common.RequestIdentity;
import com.mortal.regulation.operation.dto.InspectionSubmitDTO;
import com.mortal.regulation.operation.dto.InspectionTaskAssignDTO;
import com.mortal.regulation.operation.dto.InspectionTaskCreateDTO;
import com.mortal.regulation.operation.service.InspectionTaskService;
import com.mortal.regulation.operation.support.RequestIdentityResolver;
import com.mortal.regulation.operation.vo.InspectionTaskVO;
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
@RequestMapping("/api/regulation-operation/tasks")
public class InspectionTaskController {

    private final InspectionTaskService inspectionTaskService;
    private final RequestIdentityResolver requestIdentityResolver;

    public InspectionTaskController(InspectionTaskService inspectionTaskService,
                                    RequestIdentityResolver requestIdentityResolver) {
        this.inspectionTaskService = inspectionTaskService;
        this.requestIdentityResolver = requestIdentityResolver;
    }

    @PostMapping
    public ApiResponse<InspectionTaskVO> create(@RequestHeader("Authorization") String token,
                                                @Valid @RequestBody InspectionTaskCreateDTO dto) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        return ApiResponse.success(inspectionTaskService.createTask(identity.userId(), dto));
    }

    @PutMapping("/{id}/assign")
    public ApiResponse<InspectionTaskVO> assign(@RequestHeader("Authorization") String token,
                                                @PathVariable Long id,
                                                @Valid @RequestBody InspectionTaskAssignDTO dto) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        return ApiResponse.success(inspectionTaskService.assignTask(identity.userId(), id, dto));
    }

    @GetMapping
    public ApiResponse<PageResult<InspectionTaskVO>> list(@RequestHeader("Authorization") String token,
                                                          @RequestParam(required = false) String enterpriseName,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
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
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        return ApiResponse.success(
            inspectionTaskService.listTasksForEnforcer(identity.userId(), status, page, size)
        );
    }

    @PutMapping("/{id}/start")
    public ApiResponse<InspectionTaskVO> start(@RequestHeader("Authorization") String token,
                                               @PathVariable Long id) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        return ApiResponse.success(inspectionTaskService.startTask(identity.userId(), id));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<InspectionTaskVO> submit(@RequestHeader("Authorization") String token,
                                                @PathVariable Long id,
                                                @Valid @RequestBody InspectionSubmitDTO dto) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        return ApiResponse.success(inspectionTaskService.submitTask(identity.userId(), id, dto));
    }

    @PutMapping("/{id}/close")
    public ApiResponse<InspectionTaskVO> close(@RequestHeader("Authorization") String token,
                                               @PathVariable Long id) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        return ApiResponse.success(inspectionTaskService.closeTask(identity.userId(), id));
    }
}
