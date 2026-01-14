package com.mortal.user.controller;

import com.mortal.user.common.ApiResponse;
import com.mortal.user.dto.PublicRegisterDTO;
import com.mortal.user.dto.UserRegisterDTO;
import com.mortal.user.dto.UserUpdateDTO;
import com.mortal.user.service.UserService;
import com.mortal.user.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register/public")
    public ApiResponse<UserVO> registerPublic(@Valid @RequestBody PublicRegisterDTO dto) {
        return ApiResponse.success(userService.registerPublic(dto));
    }

    @PostMapping("/register/enterprise")
    public ApiResponse<UserVO> registerEnterprise(@Valid @RequestBody PublicRegisterDTO dto) {
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername(dto.getUsername());
        registerDTO.setPassword(dto.getPassword());
        registerDTO.setRealName(dto.getRealName());
        registerDTO.setPhone(dto.getPhone());
        registerDTO.setUserType("ENTERPRISE");
        return ApiResponse.success(userService.register(registerDTO));
    }

    @PostMapping("/register")
    public ApiResponse<UserVO> register(@Valid @RequestBody UserRegisterDTO dto) {
        return ApiResponse.success(userService.register(dto));
    }

    @PutMapping
    public ApiResponse<UserVO> update(@Valid @RequestBody UserUpdateDTO dto) {
        return ApiResponse.success(userService.updateUser(dto));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserVO> getById(@PathVariable Long id) {
        UserVO user = userService.getUserById(id);
        if (user == null) {
            return ApiResponse.failure(404, "user not found");
        }
        return ApiResponse.success(user);
    }
}
