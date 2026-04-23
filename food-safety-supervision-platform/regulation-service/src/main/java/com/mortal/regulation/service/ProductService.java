package com.mortal.regulation.service;

import com.mortal.regulation.dto.ProductSaveDTO;
import com.mortal.regulation.vo.ProductVO;
import com.mortal.regulation.vo.internal.InternalProductDetailVO;
import com.mortal.regulation.vo.internal.InternalProductSummaryVO;
import java.util.List;

public interface ProductService {

    List<ProductVO> listMyProducts(Long userId);

    ProductVO createMyProduct(Long userId, ProductSaveDTO dto);

    ProductVO updateMyProduct(Long userId, Long productId, ProductSaveDTO dto);

    List<ProductVO> listByEnterpriseId(Long enterpriseId);

    List<ProductVO> listByEnterpriseIdForRegulator(Long operatorUserId, Long enterpriseId);

    InternalProductDetailVO getInternalById(Long productId);

    List<InternalProductSummaryVO> listInternalByEnterpriseId(Long enterpriseId);

    List<InternalProductSummaryVO> getInternalSummaries(List<Long> ids);

    /**
     * 按产品名称关键字查询产品主键（模糊匹配，最多返回固定条数，供跨服务筛选）。
     *
     * @param keyword 关键字，空白则返回空列表
     * @return 产品 ID 列表，无重复
     */
    List<Long> queryInternalProductIdsByNameKeyword(String keyword);
}
