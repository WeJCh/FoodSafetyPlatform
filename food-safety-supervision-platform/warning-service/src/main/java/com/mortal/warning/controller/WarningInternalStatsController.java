package com.mortal.warning.controller;

import com.mortal.warning.common.ApiResponse;
import com.mortal.warning.dto.WarningStatsQueryDTO;
import com.mortal.warning.service.WarningStatsService;
import com.mortal.warning.vo.WarningEfficiencyStatsVO;
import com.mortal.warning.vo.WarningStatsOverviewVO;
import com.mortal.warning.vo.WarningTrendPointVO;
import com.mortal.warning.vo.WarningTypeStatsVO;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预警内部统计接口（供 query-service 调用）。
 */
@RestController
@RequestMapping("/api/warning/internal/stats")
public class WarningInternalStatsController {

    private final WarningStatsService warningStatsService;
    private final String internalToken;

    public WarningInternalStatsController(WarningStatsService warningStatsService,
                                          @Value("${warning.internal.token:warning-internal-token}")
                                          String internalToken) {
        this.warningStatsService = warningStatsService;
        this.internalToken = internalToken;
    }

    @GetMapping("/overview")
    public ApiResponse<WarningStatsOverviewVO> overview(WarningStatsQueryDTO queryDTO,
                                                        @RequestHeader(value = "X-Internal-Token", required = false)
                                                        String token) {
        if (!isAllowed(token)) {
            return ApiResponse.failure(403, "forbidden");
        }
        return ApiResponse.success(warningStatsService.getOverview(queryDTO));
    }

    @GetMapping("/trend")
    public ApiResponse<List<WarningTrendPointVO>> trend(WarningStatsQueryDTO queryDTO,
                                                        @RequestHeader(value = "X-Internal-Token", required = false)
                                                        String token) {
        if (!isAllowed(token)) {
            return ApiResponse.failure(403, "forbidden");
        }
        return ApiResponse.success(warningStatsService.getTrend(queryDTO));
    }

    @GetMapping("/types")
    public ApiResponse<List<WarningTypeStatsVO>> types(WarningStatsQueryDTO queryDTO,
                                                       @RequestHeader(value = "X-Internal-Token", required = false)
                                                       String token) {
        if (!isAllowed(token)) {
            return ApiResponse.failure(403, "forbidden");
        }
        return ApiResponse.success(warningStatsService.getTypeTop(queryDTO));
    }

    @GetMapping("/efficiency")
    public ApiResponse<WarningEfficiencyStatsVO> efficiency(WarningStatsQueryDTO queryDTO,
                                                            @RequestHeader(value = "X-Internal-Token", required = false)
                                                            String token) {
        if (!isAllowed(token)) {
            return ApiResponse.failure(403, "forbidden");
        }
        return ApiResponse.success(warningStatsService.getEfficiency(queryDTO));
    }

    private boolean isAllowed(String token) {
        return StringUtils.hasText(token) && internalToken.equals(token.trim());
    }
}

