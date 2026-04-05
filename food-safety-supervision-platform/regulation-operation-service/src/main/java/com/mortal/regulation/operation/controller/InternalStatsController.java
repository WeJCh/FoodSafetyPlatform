package com.mortal.regulation.operation.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.operation.dto.InternalStatsQueryDTO;
import com.mortal.regulation.operation.service.InternalStatsService;
import com.mortal.regulation.operation.vo.InternalOperationStatsOverviewVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 执行域内部统计接口。
 */
@RestController
@RequestMapping("/api/regulation-operation/internal/stats")
public class InternalStatsController {

    private final InternalStatsService internalStatsService;
    private final String internalToken;

    public InternalStatsController(InternalStatsService internalStatsService,
                                   @Value("${regulation-operation.internal.token:regulation-operation-internal-token}")
                                   String internalToken) {
        this.internalStatsService = internalStatsService;
        this.internalToken = internalToken;
    }
    /**
     * 获取执行域统计概览。
     * 
     * @param queryDTO 查询条件
     * @param token 内部令牌
     * @return 执行域统计概览
     */
    @GetMapping("/overview")
    public ApiResponse<InternalOperationStatsOverviewVO> overview(InternalStatsQueryDTO queryDTO,
                                                                  @RequestHeader(value = "X-Internal-Token",
                                                                      required = false) String token) {
        if (!isAllowed(token)) {
            return ApiResponse.failure(403, "forbidden");
        }
        return ApiResponse.success(internalStatsService.getOverview(queryDTO));
    }

    private boolean isAllowed(String token) {
        return StringUtils.hasText(token) && internalToken.equals(token.trim());
    }
}
