package com.mortal.regulation.vo.internal;

import lombok.Data;

@Data
public class InternalProductDetailVO {

    private Long id;
    private Long enterpriseId;
    private String productName;
    private String category;
    private String specification;
    private String status;
    private String remark;
}
