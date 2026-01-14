package com.mortal.user.controller;

import com.mortal.user.common.ApiResponse;
import com.mortal.user.entity.Role;
import com.mortal.user.service.RoleService;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ApiResponse<List<Role>> listRoles() {
        return ApiResponse.success(roleService.listRoles());
    }

    @PostMapping("/bind")
    public ApiResponse<Map<String, Object>> bindRole(@RequestParam Long userId,
                                                     @RequestParam Long roleId) {
        roleService.bindRole(userId, roleId);
        return ApiResponse.success(Map.of("success", true));
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<Long>> listUserRoles(@PathVariable Long userId) {
        return ApiResponse.success(roleService.listUserRoleIds(userId));
    }
}
