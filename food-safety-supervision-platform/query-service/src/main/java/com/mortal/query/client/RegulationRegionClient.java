package com.mortal.query.client;

import com.mortal.platform.common.ApiResponse;
import com.mortal.query.vo.RegionVO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "regulation-service", contextId = "queryRegulationRegionClient")
public interface RegulationRegionClient {

    @GetMapping("/api/regulation/regions")
    ApiResponse<List<RegionVO>> listRegions(@RequestHeader("Authorization") String authorization,
                                            @RequestParam(value = "parentId", required = false) Long parentId);
}
