package com.mortal.user.dto;

import lombok.Data;

@Data
public class LoginResult {

    private Long userId;
    private String username;
    private String userType;
    private String token;
}
