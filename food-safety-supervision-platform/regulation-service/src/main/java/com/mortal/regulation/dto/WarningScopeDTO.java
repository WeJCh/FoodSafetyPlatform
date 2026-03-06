package com.mortal.regulation.dto;

import lombok.Data;

/**
 * 预警访问作用域（转发给 warning-service）。
 */
@Data
public class WarningScopeDTO {

    /**
     * 责任执法员ID：执法员视角按本人过滤。
     */
    private Long ownerRegulatorId;

    /**
     * 辖区ID列表（逗号分隔）：管理员视角按辖区过滤。
     */
    private String regionIds;
}
