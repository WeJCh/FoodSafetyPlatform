package com.mortal.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserPasswordChangeDTO {

    @NotBlank
    private String oldPassword;

    @NotBlank
    @Size(min = 8, message = "newPassword must be at least 8 characters")
    private String newPassword;
}
