package com.mortal.user.service;

import java.time.Instant;
import java.util.List;
import lombok.Data;

@Data
public class AuthSessionCacheValue {

    private Long userId;
    private String username;
    private String userType;
    private List<String> roles;
    private Instant expireAt;
}
