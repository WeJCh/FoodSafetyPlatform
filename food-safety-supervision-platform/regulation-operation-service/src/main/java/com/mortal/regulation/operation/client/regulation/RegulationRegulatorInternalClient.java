package com.mortal.regulation.operation.client.regulation;

import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.operation.client.regulation.vo.InternalRegulatorIdentityVO;
import com.mortal.regulation.operation.client.regulation.vo.InternalRegulatorSummaryVO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "regulation-service", contextId = "regulationRegulatorInternalClient")
public interface RegulationRegulatorInternalClient {

    @GetMapping("/api/internal/regulation/regulators/by-user/{userId}")
    ApiResponse<InternalRegulatorIdentityVO> getRegulatorByUserId(@PathVariable("userId") Long userId,
                                                                  @RequestHeader("X-Internal-Token")
                                                                  String internalToken);

    @GetMapping("/api/internal/regulation/regulators/{id}/identity")
    ApiResponse<InternalRegulatorIdentityVO> getRegulatorIdentityById(@PathVariable("id") Long id,
                                                                      @RequestHeader("X-Internal-Token")
                                                                      String internalToken);

    @GetMapping("/api/internal/regulation/regulators/{id}")
    ApiResponse<InternalRegulatorSummaryVO> getRegulatorById(@PathVariable("id") Long id,
                                                             @RequestHeader("X-Internal-Token")
                                                             String internalToken);

    @PostMapping("/api/internal/regulation/regulators/summaries")
    ApiResponse<List<InternalRegulatorSummaryVO>> getRegulatorSummaries(@RequestBody List<Long> ids,
                                                                        @RequestHeader("X-Internal-Token")
                                                                        String internalToken);

    @GetMapping("/api/internal/regulation/regulators/query-ids-by-name")
    ApiResponse<List<Long>> queryRegulatorIdsByName(@RequestParam("keyword") String keyword,
                                                    @RequestHeader("X-Internal-Token")
                                                    String internalToken);

    @GetMapping("/api/internal/regulation/regulators/{id}/scope-enterprise-ids")
    ApiResponse<List<Long>> getScopeEnterpriseIds(@PathVariable("id") Long id,
                                                  @RequestHeader("X-Internal-Token")
                                                  String internalToken);

    @GetMapping("/api/internal/regulation/regulators/{id}/scope-region-ids")
    ApiResponse<List<Long>> getScopeRegionIds(@PathVariable("id") Long id,
                                              @RequestHeader("X-Internal-Token")
                                              String internalToken);

    @GetMapping("/api/internal/regulation/regulators/{id}/assignable-to-region/{regionId}")
    ApiResponse<Boolean> isAssignableToRegion(@PathVariable("id") Long id,
                                              @PathVariable("regionId") Long regionId,
                                              @RequestHeader("X-Internal-Token")
                                              String internalToken);
}
