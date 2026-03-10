package com.mortal.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 预警处理动作请求。
 */
@Data
public class WarningProcessActionDTO {

    /**
     * 动作类型：PROCESS / RESOLVE
     */
    @NotBlank(message = "actionType required")
    private String actionType;

    /**
     * 处理说明
     */
    @Size(max = 500, message = "actionComment too long")
    private String actionComment;
}
