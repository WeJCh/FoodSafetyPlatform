package com.mortal.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegulatorSelfUpdateDTO {

    @NotBlank
    private String name;

    @Pattern(regexp = "^\\d{11}$", message = "phone must be 11 digits")
    private String phone;
}
