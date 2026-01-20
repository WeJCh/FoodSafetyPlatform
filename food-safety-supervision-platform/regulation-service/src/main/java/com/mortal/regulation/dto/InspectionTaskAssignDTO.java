package com.mortal.regulation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InspectionTaskAssignDTO {

    @NotNull
    private Long regulatorId;
}
