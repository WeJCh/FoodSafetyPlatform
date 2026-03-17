package com.mortal.regulation.operation.client.regulation.vo;

import java.util.List;
import lombok.Data;

@Data
public class InternalRegulatorIdentityVO {

    private Long id;
    private Long userId;
    private String name;
    private String phone;
    private String roleType;
    private Integer status;
    private List<Long> regionIds;
}
