package com.mortal.warning.dto;

import lombok.Data;

/**
 * 预警访问作用域（用于权限过滤）。
 */
@Data
public class WarningScopeDTO {

    /**
     * 责任执法员ID：传值时仅可访问本人负责的预警。
     */
    private Long ownerRegulatorId;

    /**
     * 辖区ID列表（逗号分隔）：传值时仅可访问辖区内预警。
     */
    private String regionIds;
}
