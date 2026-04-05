package com.mortal.regulation.operation.client.regulation;

import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.operation.client.regulation.dto.EnterpriseKeyReasonUpsertDTO;
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

    /**
     * 获取企业范围ID列表。
     * 
     * @param regionId 辖区ID
     * @param regionIds 辖区ID列表
     * @param internalToken 内部令牌
     * @return 企业范围ID列表
     */
    @GetMapping("/api/internal/regulation/enterprises/scope-ids")
    ApiResponse<List<Long>> getEnterpriseIdsByScope(@RequestParam(value = "regionId", required = false) Long regionId,
                                                    @RequestParam(value = "regionIds", required = false)
                                                    String regionIds,
                                                    @RequestHeader("X-Internal-Token")
                                                    String internalToken);

    /**
     * 标记企业为关键企业
     * @param id 企业ID
     * @param dto 企业关键原因插入DTO
     * @param internalToken 内部令牌
     * @return 空响应
     */
    @PostMapping("/api/internal/regulation/enterprises/{id}/key-reasons")
    ApiResponse<Void> markEnterpriseAsKey(@PathVariable("id") Long id,
                                          @RequestBody EnterpriseKeyReasonUpsertDTO dto,
                                          @RequestHeader("X-Internal-Token")
                                          String internalToken);
}
