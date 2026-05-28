package com.mortal.warning.dto;

import lombok.Data;

/**
 * 预警访问作用域（用于权限过滤）。
 */
@Data
public class WarningScopeDTO {

    /**
     * 分派处理人ID：传值时仅可访问已分派给该执法员的预警。
     */
    private Long assignedRegulatorId;

    /**
     * 辖区ID列表（逗号分隔）：传值时仅可访问辖区内预警。
     */
    private String regionIds;
}
