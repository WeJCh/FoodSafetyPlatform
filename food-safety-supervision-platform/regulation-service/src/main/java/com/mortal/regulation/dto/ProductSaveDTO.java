package com.mortal.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import com.mortal.regulation.validation.ValidProductCategory;
import lombok.Data;

@Data
public class ProductSaveDTO {

    @NotBlank
    private String productName;

    @NotBlank
    @ValidProductCategory
    private String category;

    private String specification;
    private String status;
    private String remark;
}
