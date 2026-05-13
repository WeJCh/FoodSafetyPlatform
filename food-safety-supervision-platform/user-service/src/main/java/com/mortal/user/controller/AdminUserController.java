package com.mortal.user.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.user.dto.UserRegisterDTO;
import com.mortal.user.dto.UserUpdateDTO;
import com.mortal.user.enums.UserType;
import com.mortal.user.service.UserService;
import com.mortal.user.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
        dto.setUserType(UserType.REGULATOR.code());
        return ApiResponse.success(userService.register(dto));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserVO> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        dto.setId(id);
        return ApiResponse.success(userService.updateUser(dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.success(null);
    }
}
