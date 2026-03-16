package com.mortal.complaint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 投诉处理DTO
 */
@Data
public class ComplaintHandleDTO {

    @NotBlank
    @Size(max = 1000)
    private String handleResult;
}
