package com.mortal.regulation.operation.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.operation.common.OperationErrorMessages;
import com.mortal.regulation.operation.common.RequestIdentity;
import com.mortal.regulation.operation.service.SamplingTaskService;
import com.mortal.regulation.operation.support.RequestIdentityResolver;
import com.mortal.regulation.operation.vo.SamplingResultVO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 抽检结果管理控制器
 * 
 * @author xiezhongyuan
 * @since 2026-03-23
 */
@RestController
@RequestMapping("/api/regulation-operation/sampling/results")
public class SamplingResultManageController {

    private final SamplingTaskService samplingTaskService;
    private final RequestIdentityResolver requestIdentityResolver;

    public SamplingResultManageController(SamplingTaskService samplingTaskService,
                                          RequestIdentityResolver requestIdentityResolver) {
        this.samplingTaskService = samplingTaskService;
        this.requestIdentityResolver = requestIdentityResolver;
    }

    /**
     * 发布抽检结果
     * 
     * @param token 令牌
     * @param id 结果ID
     * @return 抽检结果VO
     */
    @PostMapping("/{id}/publish")
    public ApiResponse<SamplingResultVO> publish(@RequestHeader("Authorization") String token,
                                                 @PathVariable Long id) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        return ApiResponse.success(samplingTaskService.publishResult(identity.userId(), id));
    }

    /**
     * 下线抽检结果
     * 
     * @param token 令牌
     * @param id 结果ID
     * @return 抽检结果VO
     */
    @PostMapping("/{id}/offline")
    public ApiResponse<SamplingResultVO> offline(@RequestHeader("Authorization") String token,
                                                 @PathVariable Long id) {
        RequestIdentity identity = requestIdentityResolver.resolve(token);
        if (!identity.isRegulator()) {
            return ApiResponse.failure(403, OperationErrorMessages.REGULATOR_ONLY);
        }
        return ApiResponse.success(samplingTaskService.offlineResult(identity.userId(), id));
    }
}
