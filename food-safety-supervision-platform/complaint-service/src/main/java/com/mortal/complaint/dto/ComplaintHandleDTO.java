package com.mortal.complaint.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 投诉处理DTO
 */
@Data
public class ComplaintHandleDTO {

    @Size(max = 500)
    private String feedbackSummary;

    @Size(max = 1000)
    private String handleResult;
}
