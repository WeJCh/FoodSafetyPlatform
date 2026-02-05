package com.mortal.regulation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
/**
 * 投诉指派DTO
 */
@Data
public class ComplaintAssignDTO {

    @NotNull
    private Long regulatorId;
}
