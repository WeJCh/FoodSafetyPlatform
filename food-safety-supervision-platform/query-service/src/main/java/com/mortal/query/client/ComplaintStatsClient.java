package com.mortal.query.client;

import com.mortal.platform.common.ApiResponse;
import com.mortal.query.dto.WarningStatsQueryDTO;
import com.mortal.query.vo.ComplaintStatsOverviewVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "complaint-service", contextId = "queryComplaintStatsClient")
public interface ComplaintStatsClient {

    @GetMapping("/api/complaint/internal/stats/overview")
    ApiResponse<ComplaintStatsOverviewVO> fetchOverview(@SpringQueryMap WarningStatsQueryDTO queryDTO,
                                                        @RequestHeader("X-Internal-Token") String internalToken);
}
