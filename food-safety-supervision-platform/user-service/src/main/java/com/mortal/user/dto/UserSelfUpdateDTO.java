package com.mortal.user.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserSelfUpdateDTO {

    private String realName;

    @Pattern(regexp = "^\\d{11}$", message = "phone must be 11 digits")
    private String phone;
}
