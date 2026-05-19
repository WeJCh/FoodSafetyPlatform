package com.mortal.user.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.user.dto.PublicRegisterDTO;
import com.mortal.user.dto.UserPasswordChangeDTO;
import com.mortal.user.dto.UserSelfUpdateDTO;
import com.mortal.user.vo.AuditLogVO;
import com.mortal.user.service.UserService;
import com.mortal.user.vo.UserVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

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
        return ApiResponse.success(userService.registerEnterprise(dto));
    }

    @GetMapping("/me")
    public ApiResponse<UserVO> getCurrentUser(@RequestHeader("Authorization") String token) {
        UserVO user = userService.getCurrentUser(token);
        if (user == null) {
            return ApiResponse.failure(404, "user not found");
        }
        return ApiResponse.success(user);
    }

    @PutMapping("/me")
    public ApiResponse<UserVO> updateCurrentUser(@RequestHeader("Authorization") String token,
                                                 @Valid @RequestBody UserSelfUpdateDTO dto) {
        return ApiResponse.success(userService.updateCurrentUser(token, dto));
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changeCurrentUserPassword(@RequestHeader("Authorization") String token,
                                                       @Valid @RequestBody UserPasswordChangeDTO dto) {
        userService.changeCurrentUserPassword(token, dto);
        return ApiResponse.success(null);
    }

    @GetMapping("/me/audit-logs")
    public ApiResponse<List<AuditLogVO>> listCurrentUserAuditLogs(@RequestHeader("Authorization") String token,
                                                                  @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(userService.listCurrentUserAuditLogs(token, limit));
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
