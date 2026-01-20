package com.mortal.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InspectionItemDTO {

    @NotBlank
    @Size(max = 100)
    private String itemName;

    @Size(max = 20)
    private String itemResult;

    @Size(max = 500)
    private String problemDesc;
}
