package com.mortal.user.controller.internal;

import com.mortal.platform.common.ApiResponse;
import com.mortal.user.dto.UserUpdateDTO;
import com.mortal.user.service.UserService;
import com.mortal.user.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/users")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ApiResponse<UserVO> getById(@PathVariable Long id) {
        UserVO user = userService.getUserById(id);
        if (user == null) {
            return ApiResponse.failure(404, "user not found");
        }
        return ApiResponse.success(user);
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
