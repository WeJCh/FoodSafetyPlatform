package com.mortal.regulation.vo.internal;

import lombok.Data;

@Data
public class InternalProductSummaryVO {

    private Long id;
    private Long enterpriseId;
    private String productName;
    private String category;
    private String specification;
    private String status;
}
