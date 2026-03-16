package com.mortal.complaint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 投诉拒绝DTO
 */
@Data
public class ComplaintRejectDTO {

    @NotBlank
    @Size(max = 500)
    private String reason;
}
