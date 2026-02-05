package com.mortal.user.dto;

import lombok.Data;
import java.util.List;

@Data
public class LoginResult {

    private Long userId;
    private String username;
    private String userType;
    private String token;
    private List<String> roles;
}
