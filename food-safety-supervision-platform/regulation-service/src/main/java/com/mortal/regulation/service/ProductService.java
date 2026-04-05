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
}
