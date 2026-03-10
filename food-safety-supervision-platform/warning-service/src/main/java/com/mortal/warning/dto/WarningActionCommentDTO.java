package com.mortal.warning.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 预警动作说明请求。
 */
@Data
public class WarningActionCommentDTO {

    /**
     * 动作说明。
     */
    @Size(max = 500, message = "actionComment too long")
    private String actionComment;
}
