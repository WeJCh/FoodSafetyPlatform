package com.mortal.query.client;

import com.mortal.platform.common.ApiResponse;
import com.mortal.query.dto.WarningStatsQueryDTO;
import com.mortal.query.vo.OperationStatsOverviewVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "regulation-operation-service", contextId = "queryRegulationOperationStatsClient")
public interface RegulationOperationStatsClient {

    @GetMapping("/api/regulation-operation/internal/stats/overview")
    ApiResponse<OperationStatsOverviewVO> fetchOverview(@SpringQueryMap WarningStatsQueryDTO queryDTO,
                                                        @RequestHeader("X-Internal-Token") String internalToken);
}
