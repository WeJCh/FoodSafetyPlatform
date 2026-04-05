package com.mortal.regulation.operation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 抽检任务指派DTO
 */
@Data
public class SamplingTaskAssignDTO {

    @NotNull
    private Long regulatorId;
}
