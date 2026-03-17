package com.mortal.regulation.operation.client.regulation;

import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.operation.client.regulation.vo.InternalEnterpriseDetailVO;
import com.mortal.regulation.operation.client.regulation.vo.InternalEnterpriseSummaryVO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "regulation-service", contextId = "regulationEnterpriseInternalClient")
public interface RegulationEnterpriseInternalClient {

    @GetMapping("/api/internal/regulation/enterprises/{id}")
    ApiResponse<InternalEnterpriseDetailVO> getEnterpriseById(@PathVariable("id") Long id,
                                                              @RequestHeader("X-Internal-Token")
                                                              String internalToken);

    @GetMapping("/api/internal/regulation/enterprises/by-user/{userId}")
    ApiResponse<InternalEnterpriseDetailVO> getEnterpriseByUserId(@PathVariable("userId") Long userId,
                                                                  @RequestHeader("X-Internal-Token")
                                                                  String internalToken);

    @PostMapping("/api/internal/regulation/enterprises/summaries")
    ApiResponse<List<InternalEnterpriseSummaryVO>> getEnterpriseSummaries(@RequestBody List<Long> ids,
                                                                          @RequestHeader("X-Internal-Token")
                                                                          String internalToken);

    @GetMapping("/api/internal/regulation/enterprises/query-ids-by-name")
    ApiResponse<List<Long>> queryEnterpriseIdsByName(@RequestParam("keyword") String keyword,
                                                     @RequestHeader("X-Internal-Token")
                                                     String internalToken);
}
