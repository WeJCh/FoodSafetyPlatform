package com.mortal.regulation.vo.internal;

import lombok.Data;

@Data
public class InternalRegulatorSummaryVO {

    private Long id;
    private Long userId;
    private String name;
    private String phone;
    private String roleType;
    private Integer status;
}
