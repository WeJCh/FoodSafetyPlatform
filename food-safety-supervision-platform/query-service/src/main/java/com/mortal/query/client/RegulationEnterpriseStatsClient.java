package com.mortal.query.client;

import com.mortal.platform.common.ApiResponse;
import com.mortal.query.dto.WarningStatsQueryDTO;
import com.mortal.query.vo.EnterpriseStatsOverviewVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "regulation-service", contextId = "queryRegulationEnterpriseStatsClient")
public interface RegulationEnterpriseStatsClient {

    @GetMapping("/api/internal/regulation/enterprises/stats/overview")
    ApiResponse<EnterpriseStatsOverviewVO> fetchOverview(@SpringQueryMap WarningStatsQueryDTO queryDTO,
                                                         @RequestHeader("X-Internal-Token") String internalToken);
}
