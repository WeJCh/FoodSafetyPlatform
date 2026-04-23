package com.mortal.regulation.controller.internal;

import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.service.ProductService;
import com.mortal.regulation.support.ProductMasterCacheService;
import com.mortal.regulation.vo.internal.InternalProductDetailVO;
import com.mortal.regulation.vo.internal.InternalProductSummaryVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/regulation/products")
public class InternalProductController {

    private final ProductService productService;
    private final ProductMasterCacheService productMasterCacheService;

    public InternalProductController(ProductService productService,
                                     ProductMasterCacheService productMasterCacheService) {
        this.productService = productService;
        this.productMasterCacheService = productMasterCacheService;
    }

    /**
     * 获取产品详情
     * 
     * @param id 产品ID
     * @return 产品详情
     */
    @GetMapping("/{id}")
    public ApiResponse<InternalProductDetailVO> getById(@PathVariable Long id) {
        return ApiResponse.success(
            productMasterCacheService.getDetail(id, () -> productService.getInternalById(id))
        );
    }

    /**
     * 获取企业的产品列表
     * 
     * @param enterpriseId 企业ID
     * @return 产品列表
     */
    @GetMapping("/by-enterprise/{enterpriseId}")
    public ApiResponse<List<InternalProductSummaryVO>> listByEnterprise(@PathVariable Long enterpriseId) {
        return ApiResponse.success(productService.listInternalByEnterpriseId(enterpriseId));
    }

    /**
     * 获取产品摘要列表
     * 
     * @param ids 产品ID列表
     * @return 产品摘要列表
     */
    @PostMapping("/summaries")
    public ApiResponse<List<InternalProductSummaryVO>> summaries(@RequestBody(required = false) List<Long> ids) {
        return ApiResponse.success(
            productMasterCacheService.getSummaries(ids, productService::getInternalSummaries)
        );
    }

    /**
     * 按产品名称关键字查询产品 ID（模糊匹配），供运营服务等做全库关联筛选。
     */
    @GetMapping("/query-ids-by-name")
    public ApiResponse<List<Long>> queryIdsByName(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(productService.queryInternalProductIdsByNameKeyword(keyword));
    }
}
