package com.mortal.warning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 预警处理动作请求。
 */
@Data
public class WarningProcessActionDTO {

    /**
     * 动作类型：ACK / PROCESS / RESOLVE / CLOSE
     */
    @NotBlank(message = "actionType required")
    private String actionType;

    /**
     * 动作说明
     */
    @Size(max = 500, message = "actionComment too long")
    private String actionComment;
}
