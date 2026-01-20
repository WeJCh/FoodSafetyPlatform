package com.mortal.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class EnterpriseApprovalBatchDTO {

    @NotEmpty(message = "ids required")
    private List<Long> ids;
    @Size(max = 50, message = "regulatorName too long")
    private String regulatorName;
    @NotBlank(message = "comment is required")
    @Size(max = 200, message = "comment too long")
    private String comment;
}
