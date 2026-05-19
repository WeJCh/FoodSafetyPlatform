package com.mortal.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.user.entity.Role;
import com.mortal.user.entity.User;
import com.mortal.user.entity.UserRole;
import com.mortal.user.mapper.RoleMapper;
import com.mortal.user.mapper.UserMapper;
import com.mortal.user.mapper.UserRoleMapper;
import com.mortal.user.service.AuditLogService;
import com.mortal.user.service.AuthRedisService;
import com.mortal.user.service.RoleService;
import com.mortal.user.support.UserAuditSupport;
import java.util.List;
import java.util.Objects;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl implements RoleService {

    private static final String AUDIT_TARGET_TYPE_USER = "USER";
    private static final String AUDIT_BIZ_TYPE_USER = "USER";
    private static final String ACTION_USER_ROLE_BIND = "USER_ROLE_BIND";

    private final RoleMapper roleMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final AuthRedisService authRedisService;
    private final AuditLogService auditLogService;
    private final UserAuditSupport userAuditSupport;

    public RoleServiceImpl(RoleMapper roleMapper,
                           UserMapper userMapper,
                           UserRoleMapper userRoleMapper,
                           AuthRedisService authRedisService,
                           AuditLogService auditLogService,
                           UserAuditSupport userAuditSupport) {
        this.roleMapper = roleMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.authRedisService = authRedisService;
        this.auditLogService = auditLogService;
        this.userAuditSupport = userAuditSupport;
    }

    @Override
    public void bindRole(Long userId, Long roleId) {
        User user = userMapper.selectById(userId);
        Role role = roleMapper.selectById(roleId);
        UserAuditSupport.AuditActor operator = userAuditSupport.resolveCurrentOperator();
        List<String> beforeRoles = loadRoleCodes(userId);
        String beforeData = userAuditSupport.writeUserSnapshot(user, beforeRoles);
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRoleMapper.insert(userRole);
        List<String> afterRoles = loadRoleCodes(userId);
        auditLogService.recordAudit(
            operator == null ? null : operator.userId(),
            operator == null ? null : operator.userType(),
            operator == null ? null : operator.operatorName(),
            AUDIT_TARGET_TYPE_USER,
            userId,
            userId,
            userAuditSupport.buildTargetName(user),
            AUDIT_BIZ_TYPE_USER,
            ACTION_USER_ROLE_BIND,
            "\u7ED1\u5B9A\u89D2\u8272",
            beforeData,
            userAuditSupport.writeUserSnapshot(user, afterRoles),
            buildBindRoleRemark(role)
        );
        authRedisService.invalidateUserAllSessions(userId);
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
            .toList();
    }

    private String buildBindRoleRemark(Role role) {
        if (role == null) {
            return "\u7ED1\u5B9A\u89D2\u8272";
        }
        if (StringUtils.hasText(role.getRoleName())) {
            return "\u7ED1\u5B9A\u89D2\u8272\uFF1A" + role.getRoleName().trim();
        }
        String roleDisplayName = userAuditSupport.resolveRoleDisplayName(role.getRoleCode());
        if (StringUtils.hasText(roleDisplayName)) {
            return "\u7ED1\u5B9A\u89D2\u8272\uFF1A" + roleDisplayName;
        }
        return "\u7ED1\u5B9A\u89D2\u8272";
    }
}
