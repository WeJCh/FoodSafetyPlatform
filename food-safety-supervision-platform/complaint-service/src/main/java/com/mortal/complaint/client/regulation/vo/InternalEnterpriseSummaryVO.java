package com.mortal.complaint.client.regulation.vo;

import lombok.Data;
/**
 * 内部企业摘要VO
 */
@Data
public class InternalEnterpriseSummaryVO {

    private Long id;
    private String enterpriseName;
    private Long regionId;
    private String addressDetail;
}
