package com.mortal.regulation.operation.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.platform.common.PageResult;
import com.mortal.regulation.operation.service.SamplingTaskService;
import com.mortal.regulation.operation.vo.SamplingResultVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公众抽检结果控制器
 * 
 * @author xiezhongyuan
 * @since 2026-03-23
 */
@RestController
@RequestMapping("/api/regulation-operation/public/sampling/results")
public class PublicSamplingResultController {

    private final SamplingTaskService samplingTaskService;

    public PublicSamplingResultController(SamplingTaskService samplingTaskService) {
        this.samplingTaskService = samplingTaskService;
    }

    /**
     * 查询公众抽检结果列表
     * 
     * @param enterpriseName 企业名称
     * @param result 结果
     * @param page 页码
     * @param size 每页大小
     * @return 抽检结果列表
     */
    @GetMapping
    public ApiResponse<PageResult<SamplingResultVO>> list(@RequestParam(required = false) String enterpriseName,
                                                          @RequestParam(required = false) String result,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(samplingTaskService.listPublicResults(enterpriseName, result, page, size));
    }

    /**
     * 获取公众抽检结果详情
     * 
     * @param id 结果ID
     * @return 抽检结果VO
     */
    @GetMapping("/{id}")
    public ApiResponse<SamplingResultVO> detail(@PathVariable Long id) {
        SamplingResultVO detail = samplingTaskService.getPublicResultDetail(id);
        if (detail == null) {
            return ApiResponse.failure(404, "public sampling result not found");
        }
        return ApiResponse.success(detail);
    }
}
