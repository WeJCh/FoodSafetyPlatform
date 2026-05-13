package com.mortal.complaint.client.regulation.vo;

import java.util.List;
import lombok.Data;

/**
 * 内部监管者身份VO
 */
@Data
public class InternalRegulatorIdentityVO {

    private Long id;
    private Long userId;
    private String name;
    private String username;
    private String phone;
    private String roleType;
    private Integer status;
    private List<Long> regionIds;
}
