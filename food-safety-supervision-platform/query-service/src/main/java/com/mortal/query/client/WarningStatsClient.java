package com.mortal.query.client;

import com.mortal.platform.common.ApiResponse;
import com.mortal.query.dto.WarningStatsQueryDTO;
import com.mortal.query.vo.WarningEfficiencyStatsVO;
import com.mortal.query.vo.WarningStatsOverviewVO;
import com.mortal.query.vo.WarningTrendPointVO;
import com.mortal.query.vo.WarningTypeStatsVO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient("warning-service")
public interface WarningStatsClient {

    @GetMapping("/api/warning/internal/stats/overview")
    ApiResponse<WarningStatsOverviewVO> fetchOverview(@SpringQueryMap WarningStatsQueryDTO queryDTO,
                                                      @RequestHeader("X-Internal-Token") String internalToken);

    @GetMapping("/api/warning/internal/stats/trend")
    ApiResponse<List<WarningTrendPointVO>> fetchTrend(@SpringQueryMap WarningStatsQueryDTO queryDTO,
                                                      @RequestHeader("X-Internal-Token") String internalToken);

    @GetMapping("/api/warning/internal/stats/types")
    ApiResponse<List<WarningTypeStatsVO>> fetchTypes(@SpringQueryMap WarningStatsQueryDTO queryDTO,
                                                     @RequestHeader("X-Internal-Token") String internalToken);

    @GetMapping("/api/warning/internal/stats/efficiency")
    ApiResponse<WarningEfficiencyStatsVO> fetchEfficiency(@SpringQueryMap WarningStatsQueryDTO queryDTO,
                                                          @RequestHeader("X-Internal-Token") String internalToken);
}

