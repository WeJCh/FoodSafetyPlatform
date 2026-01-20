package com.mortal.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EnterpriseApprovalDTO {

    @Size(max = 50, message = "regulatorName too long")
    private String regulatorName;
    @NotBlank(message = "comment is required")
    @Size(max = 200, message = "comment too long")
    private String comment;
}
