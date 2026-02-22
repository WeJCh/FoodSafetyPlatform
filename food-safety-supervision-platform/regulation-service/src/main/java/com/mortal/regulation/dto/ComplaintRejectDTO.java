package com.mortal.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 投诉驳回DTO
 */
@Data
public class ComplaintRejectDTO {

    @NotBlank
    @Size(max = 500)
    private String reason;
}
