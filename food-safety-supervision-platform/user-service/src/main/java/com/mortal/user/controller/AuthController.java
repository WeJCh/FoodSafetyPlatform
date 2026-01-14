package com.mortal.user.controller;

import com.mortal.user.common.ApiResponse;
import com.mortal.user.dto.LoginDTO;
import com.mortal.user.service.AuthService;
import com.mortal.user.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        var result = userService.login(dto);
        return ApiResponse.success(Map.of(
            "token", result.getToken(),
            "userId", result.getUserId(),
            "username", result.getUsername(),
            "userType", result.getUserType()
        ));
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, Object>> logout(@RequestHeader("Authorization") String token) {
        userService.logout(token);
        return ApiResponse.success(Map.of("success", true));
    }

    @PostMapping("/verify")
    public ApiResponse<Map<String, Object>> verify(@RequestHeader("Authorization") String token) {
        return ApiResponse.success(Map.of("valid", authService.verifyToken(token)));
    }
}
