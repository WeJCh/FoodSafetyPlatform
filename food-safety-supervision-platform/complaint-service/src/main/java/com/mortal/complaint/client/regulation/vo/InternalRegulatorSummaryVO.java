package com.mortal.complaint.client.regulation.vo;

import lombok.Data;

/**
 * 内部监管者摘要VO
 */
@Data
public class InternalRegulatorSummaryVO {

    private Long id;
    private Long userId;
    private String name;
    private String phone;
    private String roleType;
    private Integer status;
}
