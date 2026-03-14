package com.mortal.query.client;

import com.mortal.query.common.ApiResponse;
import com.mortal.query.vo.RegulatorProfileVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "regulation-service", contextId = "queryRegulatorProfileClient")
public interface RegulatorProfileClient {

    @GetMapping("/api/regulation/regulators/me")
    ApiResponse<RegulatorProfileVO> getMyProfile(@RequestHeader("Authorization") String token);
}
