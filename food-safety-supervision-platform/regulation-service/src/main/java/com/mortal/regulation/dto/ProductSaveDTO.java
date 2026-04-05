package com.mortal.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductSaveDTO {

    @NotBlank
    private String productName;

    @NotBlank
    private String category;

    private String specification;
    private String status;
    private String remark;
}
