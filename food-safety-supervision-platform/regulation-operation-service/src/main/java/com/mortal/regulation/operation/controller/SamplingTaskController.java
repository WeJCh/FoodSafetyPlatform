package com.mortal.regulation.operation.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.operation.common.OperationErrorMessages;
import com.mortal.regulation.operation.common.RequestIdentity;
import com.mortal.regulation.operation.dto.SamplingResultSubmitDTO;
import com.mortal.regulation.operation.dto.SamplingTaskAssignDTO;
import com.mortal.regulation.operation.dto.SamplingTaskCreateDTO;
import com.mortal.regulation.operation.service.SamplingTaskService;
import com.mortal.regulation.operation.support.RequestIdentityResolver;
import com.mortal.regulation.operation.vo.SamplingResultVO;
import com.mortal.regulation.operation.vo.SamplingTaskVO;
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
@RequestMapping("/api/regulation-operation/sampling/tasks")
public class SamplingTaskController {

    private final SamplingTaskService samplingTaskService;
    private final RequestIdentityResolver requestIdentityResolver;

    public SamplingTaskController(SamplingTaskService samplingTaskService,
                                  RequestIdentityResolver requestIdentityResolver) {
        this.samplingTaskService = samplingTaskService;
        this.requestIdentityResolver = requestIdentityResolver;
    }
    /**
     * 创建抽检任务
     * 
     * @param token 令牌
     * @param dto 创建DTO
     * @return 抽检任务VO
     */
    @PostMapping
    public ApiResponse<SamplingTaskVO> create(@RequestHeader("Authorization") String token,
                                              @Valid @RequestBody SamplingTaskCreateDTO dto) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        return ApiResponse.success(samplingTaskService.createTask(identity.userId(), dto));
    }

    /**
     * 指派抽检任务
     * 
     * @param token 令牌
     * @param id 任务ID
     * @param dto 指派DTO
     * @return 抽检任务VO
     */
    @PutMapping("/{id}/assign")
    public ApiResponse<SamplingTaskVO> assign(@RequestHeader("Authorization") String token,
                                              @PathVariable Long id,
                                              @Valid @RequestBody SamplingTaskAssignDTO dto) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        return ApiResponse.success(samplingTaskService.assignTask(identity.userId(), id, dto));
    }

    /**
     * 查询区域管理员抽检任务列表
     * 
     * @param token 令牌
     * @param enterpriseName 企业名称
     * @param status 任务状态
     * @param page 页码
     * @param size 每页大小
     * @return 抽检任务列表
     */
    @GetMapping
    public ApiResponse<PageResult<SamplingTaskVO>> list(@RequestHeader("Authorization") String token,
                                                        @RequestParam(required = false) String enterpriseName,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        return ApiResponse.success(
            samplingTaskService.listTasksForAdmin(identity.userId(), enterpriseName, status, page, size)
        );
    }

    /**
     * 查询执法人员抽检任务列表
     * 
     * @param token 令牌
     * @param status 任务状态
     * @param page 页码
     * @param size 每页大小
     * @return 抽检任务列表
     */
    @GetMapping("/my")
    public ApiResponse<PageResult<SamplingTaskVO>> listMy(@RequestHeader("Authorization") String token,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        return ApiResponse.success(
            samplingTaskService.listTasksForEnforcer(identity.userId(), status, page, size)
        );
    }

    /**
     * 提交抽检结果
     * 
     * @param token 令牌
     * @param id 任务ID
     * @param dto 提交DTO
     * @return 抽检结果VO
     */
    @PostMapping("/{id}/result")
    public ApiResponse<SamplingResultVO> submitResult(@RequestHeader("Authorization") String token,
                                                      @PathVariable Long id,
                                                      @Valid @RequestBody SamplingResultSubmitDTO dto) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        return ApiResponse.success(samplingTaskService.submitResult(identity.userId(), id, dto));
    }

    /**
     * 关闭抽检任务
     * 
     * @param token 令牌
     * @param id 任务ID
     * @return 抽检任务VO
     */
    @PutMapping("/{id}/close")
    public ApiResponse<SamplingTaskVO> close(@RequestHeader("Authorization") String token,
                                             @PathVariable Long id) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        return ApiResponse.success(samplingTaskService.closeTask(identity.userId(), id));
    }
}
