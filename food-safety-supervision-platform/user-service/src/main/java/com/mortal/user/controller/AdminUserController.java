package com.mortal.user.controller;

import com.mortal.user.common.ApiResponse;
import com.mortal.user.dto.UserRegisterDTO;
import com.mortal.user.service.UserService;
import com.mortal.user.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/regulators")
    public ApiResponse<UserVO> createRegulator(@Valid @RequestBody UserRegisterDTO dto) {
        dto.setUserType("REGULATOR");
        return ApiResponse.success(userService.register(dto));
    }
}
