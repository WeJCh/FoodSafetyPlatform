package com.mortal.query.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.query.dto.WarningStatsQueryDTO;
import com.mortal.query.service.WarningStatsQueryService;
import com.mortal.query.service.WarningStatsScopeService;
import com.mortal.query.support.QueryRateLimitService;
import com.mortal.query.vo.WarningEfficiencyStatsVO;
import com.mortal.query.vo.WarningStatsOverviewVO;
import com.mortal.query.vo.WarningTrendPointVO;
import com.mortal.query.vo.WarningTypeStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import org.springdoc.core.annotations.ParameterObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预警统计查询接口（监管看板）。
 *
 * <p>当前阶段 query-service 仅承载预警统计聚合能力，不扩展企业、检查、投诉综合统计接口。</p>
 */
@RestController
@RequestMapping("/api/query/warnings")
@Tag(name = "预警统计", description = "监管看板预警统计接口。时间参数统一使用 yyyy-MM-dd'T'HH:mm:ss（UTC+8 本地时间）。")
public class WarningStatsController {

    private static final Logger log = LoggerFactory.getLogger(WarningStatsController.class);

    private final WarningStatsQueryService warningStatsQueryService;
    private final WarningStatsScopeService warningStatsScopeService;
    private final QueryRateLimitService queryRateLimitService;
    private final boolean cacheEnabled;
    private final int cacheTtlSeconds;

    public WarningStatsController(WarningStatsQueryService warningStatsQueryService,
                                  WarningStatsScopeService warningStatsScopeService,
                                  QueryRateLimitService queryRateLimitService,
                                  @Value("${query.warning-stats.cache.enabled:false}") boolean cacheEnabled,
                                  @Value("${query.warning-stats.cache.ttl-seconds:30}") int cacheTtlSeconds) {
        this.warningStatsQueryService = warningStatsQueryService;
        this.warningStatsScopeService = warningStatsScopeService;
        this.queryRateLimitService = queryRateLimitService;
        this.cacheEnabled = cacheEnabled;
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    @Operation(
        summary = "预警总览统计",
        description = "返回总数、状态分布、档位分布（L1 初发 / L2 升级）。已处理完成口径：RESOLVED + CLOSED。"
    )
    @GetMapping("/overview")
    public ApiResponse<WarningStatsOverviewVO> overview(@ParameterObject WarningStatsQueryDTO queryDTO,
                                                        @RequestHeader(value = "X-User-Id", required = false)
                                                        Long userId,
                                                        @RequestHeader(value = "X-User-Type", required = false)
                                                        String userType,
                                                        @RequestHeader(value = "Authorization", required = false)
                                                        String authorization) {
        return executeWithMetrics(
            "overview",
            queryDTO,
            userId,
            userType,
            authorization,
            warningStatsQueryService::getOverview,
            data -> data == null ? 0L : (data.getTotalCount() == null ? 0L : data.getTotalCount())
        );
    }

    @Operation(
        summary = "预警趋势统计",
        description = "返回按天趋势，时间锚点为 firstOccurTime。未传 startTime/endTime 时按 trendDays（默认 7 天）统计。"
    )
    @GetMapping("/trend")
    public ApiResponse<List<WarningTrendPointVO>> trend(@ParameterObject WarningStatsQueryDTO queryDTO,
                                                        @RequestHeader(value = "X-User-Id", required = false)
                                                        Long userId,
                                                        @RequestHeader(value = "X-User-Type", required = false)
                                                        String userType,
                                                        @RequestHeader(value = "Authorization", required = false)
                                                        String authorization) {
        return executeWithMetrics(
            "trend",
            queryDTO,
            userId,
            userType,
            authorization,
            warningStatsQueryService::getTrend,
            data -> data == null ? 0L : data.size()
        );
    }

    @Operation(
        summary = "预警类型 Top 统计",
        description = "按 warningType 原值分组统计，默认返回 Top5，不做中文 label 映射。"
    )
    @GetMapping("/types")
    public ApiResponse<List<WarningTypeStatsVO>> types(@ParameterObject WarningStatsQueryDTO queryDTO,
                                                       @RequestHeader(value = "X-User-Id", required = false)
                                                       Long userId,
                                                       @RequestHeader(value = "X-User-Type", required = false)
                                                       String userType,
                                                       @RequestHeader(value = "Authorization", required = false)
                                                       String authorization) {
        return executeWithMetrics(
            "types",
            queryDTO,
            userId,
            userType,
            authorization,
            warningStatsQueryService::getTypeTop,
            data -> data == null ? 0L : data.size()
        );
    }

    @Operation(
        summary = "预警处置效率统计",
        description = "超时待处理口径：OPEN/PROCESSING 且 firstOccurTime <= now - overdueHours。"
    )
    @GetMapping("/efficiency")
    public ApiResponse<WarningEfficiencyStatsVO> efficiency(@ParameterObject WarningStatsQueryDTO queryDTO,
                                                            @RequestHeader(value = "X-User-Id", required = false)
                                                            Long userId,
                                                            @RequestHeader(value = "X-User-Type", required = false)
                                                            String userType,
                                                            @RequestHeader(value = "Authorization", required = false)
                                                            String authorization) {
        return executeWithMetrics(
            "efficiency",
            queryDTO,
            userId,
            userType,
            authorization,
            warningStatsQueryService::getEfficiency,
            data -> data == null ? 0L : 1L
        );
    }

    private <T> ApiResponse<T> executeWithMetrics(String endpoint,
                                                  WarningStatsQueryDTO queryDTO,
                                                  Long userId,
                                                  String userType,
                                                  String authorization,
                                                  Function<WarningStatsQueryDTO, T> executor,
                                                  ToLongFunction<T> countResolver) {
        long startMillis = System.currentTimeMillis();
        if (userId == null || !queryRateLimitService.isWarningStatsAllowed(userId, endpoint)) {
            return ApiResponse.failure(429, "query requests are too frequent");
        }
        WarningStatsQueryDTO scopedQuery = warningStatsScopeService.applyScope(queryDTO, userId, userType, authorization);
        String filterSummary = summarizeFilters(scopedQuery);
        try {
            T data = executor.apply(scopedQuery);
            long elapsed = System.currentTimeMillis() - startMillis;
            long resultSize = countResolver.applyAsLong(data);
            log.info(
                "warning-stats endpoint={} elapsedMs={} resultSize={} cacheEnabled={} cacheTtlSeconds={} filters={}",
                endpoint,
                elapsed,
                resultSize,
                cacheEnabled,
                cacheTtlSeconds,
                filterSummary
            );
            return ApiResponse.success(data);
        } catch (RuntimeException ex) {
            long elapsed = System.currentTimeMillis() - startMillis;
            log.warn(
                "warning-stats endpoint={} elapsedMs={} failed=true filters={} message={}",
                endpoint,
                elapsed,
                filterSummary,
                ex.getMessage()
            );
            throw ex;
        }
    }

    private String summarizeFilters(WarningStatsQueryDTO query) {
        if (query == null) {
            return "-";
        }
        return String.format(
            "start=%s,end=%s,warningType=%s,bizType=%s,level=%s,status=%s,regionId=%s,regionIds=%s,assignedTo=%s,topN=%s,trendDays=%s,overdueHours=%s",
            textOrDash(query.getStartTime()),
            textOrDash(query.getEndTime()),
            textOrDash(query.getWarningType()),
            textOrDash(query.getBizType()),
            textOrDash(query.getLevel()),
            textOrDash(query.getStatus()),
            textOrDash(query.getRegionId()),
            textOrDash(query.getRegionIds()),
            textOrDash(query.getAssignedTo()),
            textOrDash(query.getTopN()),
            textOrDash(query.getTrendDays()),
            textOrDash(query.getOverdueHours())
        );
    }

    private String textOrDash(Object value) {
        if (value == null) {
            return "-";
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text : "-";
    }
}
