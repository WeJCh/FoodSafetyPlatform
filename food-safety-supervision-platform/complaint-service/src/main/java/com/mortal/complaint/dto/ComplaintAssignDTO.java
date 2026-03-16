package com.mortal.complaint.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 投诉分配DTO
 */
@Data
public class ComplaintAssignDTO {

    @NotNull
    private Long regulatorId;
}
