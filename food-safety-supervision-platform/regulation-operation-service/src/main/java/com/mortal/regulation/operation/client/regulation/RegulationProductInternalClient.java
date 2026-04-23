package com.mortal.regulation.operation.client.regulation;

import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.operation.client.regulation.vo.InternalProductDetailVO;
import com.mortal.regulation.operation.client.regulation.vo.InternalProductSummaryVO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "regulation-service", contextId = "regulationProductInternalClient")
public interface RegulationProductInternalClient {
    /**
     * 获取产品详情
     * 
     * @param id 产品ID
     * @param internalToken 内部令牌
     * @return 产品详情
     */
    @GetMapping("/api/internal/regulation/products/{id}")
    ApiResponse<InternalProductDetailVO> getProductById(@PathVariable("id") Long id,
                                                        @RequestHeader("X-Internal-Token")
                                                        String internalToken);

    /**
     * 获取企业的产品列表
     * 
     * @param enterpriseId 企业ID
     * @param internalToken 内部令牌
     * @return 产品列表
     */
    @GetMapping("/api/internal/regulation/products/by-enterprise/{enterpriseId}")
    ApiResponse<List<InternalProductSummaryVO>> listByEnterprise(@PathVariable("enterpriseId") Long enterpriseId,
                                                                 @RequestHeader("X-Internal-Token")
                                                                 String internalToken);

    /**
     * 获取产品摘要列表
     * 
     * @param ids 产品ID列表
     * @param internalToken 内部令牌
     * @return 产品摘要列表
     */
    @PostMapping("/api/internal/regulation/products/summaries")
    ApiResponse<List<InternalProductSummaryVO>> getProductSummaries(@RequestBody List<Long> ids,
                                                                    @RequestHeader("X-Internal-Token")
                                                                    String internalToken);

    @GetMapping("/api/internal/regulation/products/query-ids-by-name")
    ApiResponse<List<Long>> queryProductIdsByName(@RequestParam("keyword") String keyword,
                                                  @RequestHeader("X-Internal-Token")
                                                  String internalToken);
}
