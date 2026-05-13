package com.mortal.warning.client.regulation;

import com.mortal.platform.common.ApiResponse;
import com.mortal.warning.client.regulation.vo.InternalRegulatorSummaryVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "regulation-service", contextId = "warningRegulationRegulatorInternalClient")
public interface RegulationRegulatorInternalClient {

    @GetMapping("/api/internal/regulation/regulators/{id}")
    ApiResponse<InternalRegulatorSummaryVO> getRegulatorById(@PathVariable("id") Long id,
                                                             @RequestHeader("X-Internal-Token")
                                                             String internalToken);
}
