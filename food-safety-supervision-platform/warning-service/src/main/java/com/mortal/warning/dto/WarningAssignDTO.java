package com.mortal.warning.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 预警指派请求。
 */
@Data
public class WarningAssignDTO {

    /**
     * 指派处理人（监管档案ID）。
     */
    @NotNull(message = "assignedTo required")
    private Long assignedTo;

    /**
     * 指派说明。
     */
    @Size(max = 500, message = "actionComment too long")
    private String actionComment;
}
