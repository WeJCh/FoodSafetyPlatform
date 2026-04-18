package com.mortal.query.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.query.dto.WarningStatsQueryDTO;
import com.mortal.query.service.SupervisionOverviewQueryService;
import com.mortal.query.service.WarningStatsScopeService;
import com.mortal.query.support.QueryRateLimitService;
import com.mortal.query.vo.SupervisionOverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 监管概览聚合接口。
 */
@RestController
@RequestMapping("/api/query/supervision")
@Tag(name = "监管概览", description = "监管端业务概览统计接口。")
public class SupervisionOverviewController {

    private static final Logger log = LoggerFactory.getLogger(SupervisionOverviewController.class);

    private final SupervisionOverviewQueryService supervisionOverviewQueryService;
    private final WarningStatsScopeService warningStatsScopeService;
    private final QueryRateLimitService queryRateLimitService;

    public SupervisionOverviewController(SupervisionOverviewQueryService supervisionOverviewQueryService,
                                         WarningStatsScopeService warningStatsScopeService,
                                         QueryRateLimitService queryRateLimitService) {
        this.supervisionOverviewQueryService = supervisionOverviewQueryService;
        this.warningStatsScopeService = warningStatsScopeService;
        this.queryRateLimitService = queryRateLimitService;
    }
    /**
     * 获取监管概览统计。
     * 
     * @param queryDTO 查询条件
     * @param userId 用户ID
     * @param userType 用户类型
     * @param authorization 授权
     * @return 监管概览统计
     */
    @Operation(summary = "监管概览统计", description = "返回企业、检查、抽检、投诉和待处理预警的聚合概览。")
    @GetMapping("/overview")
    public ApiResponse<SupervisionOverviewVO> overview(@ParameterObject WarningStatsQueryDTO queryDTO,
                                                       @RequestHeader(value = "X-User-Id", required = false)
                                                       Long userId,
                                                       @RequestHeader(value = "X-User-Type", required = false)
                                                       String userType,
                                                       @RequestHeader(value = "Authorization", required = false)
                                                       String authorization) {
        long startMillis = System.currentTimeMillis();
        if (userId == null || !queryRateLimitService.isSupervisionOverviewAllowed(userId)) {
            return ApiResponse.failure(429, "query requests are too frequent");
        }
        WarningStatsQueryDTO scopedQuery =
            warningStatsScopeService.applyScope(queryDTO, userId, userType, authorization);
        SupervisionOverviewVO overview = supervisionOverviewQueryService.getOverview(scopedQuery);
        long elapsed = System.currentTimeMillis() - startMillis;
        log.info(
            "supervision-overview elapsedMs={} regionId={} regionIds={} ownerRegulatorId={}",
            elapsed,
            scopedQuery.getRegionId(),
            scopedQuery.getRegionIds(),
            scopedQuery.getOwnerRegulatorId()
        );
        return ApiResponse.success(overview);
    }
}
