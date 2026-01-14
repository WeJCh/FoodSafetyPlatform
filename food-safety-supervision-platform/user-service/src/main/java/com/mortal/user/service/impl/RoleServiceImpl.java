package com.mortal.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.user.entity.Role;
import com.mortal.user.entity.UserRole;
import com.mortal.user.mapper.RoleMapper;
import com.mortal.user.mapper.UserRoleMapper;
import com.mortal.user.service.RoleService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public RoleServiceImpl(RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public void bindRole(Long userId, Long roleId) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRoleMapper.insert(userRole);
    }

    @Override
    public List<Role> listRoles() {
        return roleMapper.selectList(null);
    }

    @Override
    public List<Long> listUserRoleIds(Long userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId))
            .stream()
            .map(UserRole::getRoleId)
            .toList();
    }
}
