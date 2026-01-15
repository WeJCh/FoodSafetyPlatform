package com.mortal.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegulatorProfileDTO {

    @NotNull
    private Long userId;
    @NotBlank
    private String name;
    @Pattern(regexp = "^\\d{11}$", message = "phone must be 11 digits")
    private String phone;
    @NotBlank
    private String roleType;
    @NotBlank
    private String jurisdictionArea;
    private Integer status;
    private String workIdUrl;
}
