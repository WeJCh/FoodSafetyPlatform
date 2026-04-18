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
import com.mortal.user.service.AuthRedisService;
import com.mortal.user.service.AuthSessionCacheValue;
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
    private final AuthRedisService authRedisService;

    public AuthServiceImpl(TokenUtil tokenUtil,
                           UserMapper userMapper,
                           UserRoleMapper userRoleMapper,
                           RoleMapper roleMapper,
                           AuthRedisService authRedisService) {
        this.tokenUtil = tokenUtil;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.authRedisService = authRedisService;
    }

    @Override
    public boolean verifyToken(String token) {
        if (!tokenUtil.verify(token)) {
            return false;
        }
        return !authRedisService.isBlacklisted(tokenUtil.getJti(token));
    }

    @Override
    public AuthIntrospectVO introspect(String token) {
        AuthIntrospectVO result = new AuthIntrospectVO();
        result.setValid(false);
        result.setRoles(List.of());

        if (!tokenUtil.verify(token)) {
            return result;
        }
        String jti = tokenUtil.getJti(token);
        if (authRedisService.isBlacklisted(jti)) {
            return result;
        }
        String tokenHash = tokenUtil.getTokenHash(token);
        AuthIntrospectVO cached = authRedisService.getCachedIntrospect(tokenHash);
        if (cached != null) {
            return cached;
        }
        AuthSessionCacheValue session = authRedisService.getSession(jti);
        if (session != null) {
            fillIdentity(result, session, true);
            cacheIntrospect(tokenHash, token, result);
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
            cacheIntrospect(tokenHash, token, result);
            return result;
        }

        fillIdentity(result, user, loadRoleCodes(userId), true);
        cacheSession(token, user, result.getRoles());
        cacheIntrospect(tokenHash, token, result);
        return result;
    }

    private void cacheIntrospect(String tokenHash, String token, AuthIntrospectVO result) {
        long ttlSeconds = Math.min(tokenUtil.getRemainingSeconds(token), 300L);
        if (ttlSeconds <= 0) {
            return;
        }
        authRedisService.cacheIntrospect(tokenHash, result, ttlSeconds);
    }

    private void cacheSession(String token, User user, List<String> roles) {
        String jti = tokenUtil.getJti(token);
        long ttlSeconds = tokenUtil.getRemainingSeconds(token);
        if (!StringUtils.hasText(jti) || ttlSeconds <= 0 || user == null) {
            return;
        }
        AuthSessionCacheValue session = new AuthSessionCacheValue();
        session.setUserId(user.getId());
        session.setUsername(user.getUsername());
        session.setUserType(user.getUserType());
        session.setRoles(roles == null ? List.of() : roles);
        session.setExpireAt(tokenUtil.getExpireAt(token));
        authRedisService.saveSession(jti, session, ttlSeconds);
        authRedisService.bindUserJti(user.getId(), jti, ttlSeconds);
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

    private void fillIdentity(AuthIntrospectVO target,
                              AuthSessionCacheValue session,
                              boolean valid) {
        target.setValid(valid);
        target.setUserId(session.getUserId());
        target.setUsername(session.getUsername());
        target.setUserType(session.getUserType());
        target.setStatus(UserStatus.ENABLED.code());
        target.setDeleted(0);
        target.setRoles(session.getRoles() == null ? List.of() : session.getRoles());
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
