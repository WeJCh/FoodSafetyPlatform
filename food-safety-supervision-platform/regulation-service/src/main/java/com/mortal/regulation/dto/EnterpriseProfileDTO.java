package com.mortal.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class EnterpriseProfileDTO {

    @NotBlank
    private String enterpriseName;
    private String licenseNo;
    @NotNull
    private Long regionId;
    @NotBlank
    private String addressDetail;
    private String principal;
    @Pattern(regexp = "^\\d{11}$", message = "principalPhone must be 11 digits")
    private String principalPhone;
}
