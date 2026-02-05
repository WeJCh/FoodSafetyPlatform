package com.mortal.regulation.vo;

import java.util.List;
import lombok.Data;

/**
 * 投诉详情VO
 */
@Data
public class ComplaintDetailVO {

    private ComplaintVO complaint;
    private EnterpriseProfileVO enterprise;
    private List<ComplaintHandleVO> handles;
}
