package com.mortal.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRegisterDTO {

    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @JsonAlias("fullName")
    private String realName;
    @Pattern(regexp = "^\\d{11}$", message = "phone must be 11 digits")
    private String phone;
    @NotBlank
    private String userType;
    @JsonAlias("roleType")
    private String roleCode;
}
