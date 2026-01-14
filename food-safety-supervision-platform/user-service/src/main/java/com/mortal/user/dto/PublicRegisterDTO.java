package com.mortal.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublicRegisterDTO {

    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private String realName;
    @Pattern(regexp = "^\\d{11}$", message = "phone must be 11 digits")
    private String phone;
}
