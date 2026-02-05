package com.mortal.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.user.entity.Role;
import com.mortal.user.entity.User;
import com.mortal.user.entity.UserRole;
import com.mortal.user.enums.UserStatus;
import com.mortal.user.mapper.RoleMapper;
import com.mortal.user.mapper.UserMapper;
import com.mortal.user.mapper.UserRoleMapper;
import com.mortal.user.service.AuthService;
import com.mortal.user.util.TokenUtil;
import com.mortal.user.vo.AuthIntrospectVO;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthServiceImpl implements AuthService {

    private final TokenUtil tokenUtil;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    public AuthServiceImpl(TokenUtil tokenUtil,
                           UserMapper userMapper,
                           UserRoleMapper userRoleMapper,
                           RoleMapper roleMapper) {
        this.tokenUtil = tokenUtil;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public boolean verifyToken(String token) {
        return tokenUtil.verify(token);
    }

    @Override
    public AuthIntrospectVO introspect(String token) {
        AuthIntrospectVO result = new AuthIntrospectVO();
        result.setValid(false);
        result.setRoles(List.of());

        if (!tokenUtil.verify(token)) {
            return result;
        }
        Long userId = tokenUtil.getUserId(token);
        if (userId == null) {
            return result;
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getId, userId)
            .eq(User::getDeleted, 0));
        if (user == null) {
            return result;
        }

        boolean enabled = user.getStatus() == null || Objects.equals(user.getStatus(), UserStatus.ENABLED.code());
        boolean notDeleted = user.getDeleted() == null || !Objects.equals(user.getDeleted(), 1);
        if (!enabled || !notDeleted) {
            // Return identity details for observability, but mark as invalid.
            fillIdentity(result, user, loadRoleCodes(userId), false);
            return result;
        }

        fillIdentity(result, user, loadRoleCodes(userId), true);
        return result;
    }

    private void fillIdentity(AuthIntrospectVO target,
                              User user,
                              List<String> roles,
                              boolean valid) {
        target.setValid(valid);
        target.setUserId(user.getId());
        target.setUsername(user.getUsername());
        target.setUserType(user.getUserType());
        target.setStatus(user.getStatus());
        target.setDeleted(user.getDeleted());
        target.setRoles(roles);
    }

    private List<String> loadRoleCodes(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getDeleted, 0))
            .stream()
            .map(UserRole::getRoleId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectBatchIds(roleIds)
            .stream()
            .filter(Objects::nonNull)
            .filter(role -> role.getDeleted() == null || !Objects.equals(role.getDeleted(), 1))
            .map(Role::getRoleCode)
            .filter(StringUtils::hasText)
            .distinct()
            .collect(Collectors.toList());
    }
}
