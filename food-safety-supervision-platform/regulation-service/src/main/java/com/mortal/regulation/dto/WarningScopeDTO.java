package com.mortal.regulation.dto;

import lombok.Data;

/**
 * 预警访问作用域（转发给 warning-service）。
 */
@Data
public class WarningScopeDTO {

    /**
     * 分派处理人ID：执法员仅可访问已分派给自己的预警。
     */
    private Long assignedRegulatorId;

    /**
     * 辖区ID列表（逗号分隔）：管理员按辖区过滤。
     */
    private String regionIds;
}
