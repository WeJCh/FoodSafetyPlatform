package com.mortal.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mortal.user.dto.LoginDTO;
import com.mortal.user.dto.LoginResult;
import com.mortal.user.dto.PublicRegisterDTO;
import com.mortal.user.dto.UserPasswordChangeDTO;
import com.mortal.user.dto.UserRegisterDTO;
import com.mortal.user.dto.UserSelfUpdateDTO;
import com.mortal.user.dto.UserUpdateDTO;
import com.mortal.user.entity.Role;
import com.mortal.user.entity.User;
import com.mortal.user.entity.UserRole;
import com.mortal.user.enums.RoleCode;
import com.mortal.user.enums.UserStatus;
import com.mortal.user.enums.UserType;
import com.mortal.user.mapper.RoleMapper;
import com.mortal.user.mapper.UserMapper;
import com.mortal.user.mapper.UserRoleMapper;
import com.mortal.user.service.AuditLogService;
import com.mortal.user.service.AuthRedisService;
import com.mortal.user.service.AuthSessionCacheValue;
import com.mortal.user.service.UserService;
import com.mortal.user.support.UserAuditSupport;
import com.mortal.user.util.PasswordEncoderUtil;
import com.mortal.user.util.TokenUtil;
import com.mortal.user.vo.AuditLogVO;
import com.mortal.user.vo.UserVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {

    private static final String AUDIT_TARGET_TYPE_USER = "USER";
    private static final String AUDIT_BIZ_TYPE_USER = "USER";
    private static final String AUDIT_TARGET_TYPE_LOGIN_IDENTIFIER = "LOGIN_IDENTIFIER";
    private static final String AUDIT_BIZ_TYPE_AUTH = "AUTH";
    private static final String ACTION_USER_REGISTER = "USER_REGISTER";
    private static final String ACTION_USER_REGISTER_PUBLIC = "USER_REGISTER_PUBLIC";
    private static final String ACTION_USER_REGISTER_ENTERPRISE = "USER_REGISTER_ENTERPRISE";
    private static final String ACTION_USER_CREATE_REGULATOR = "USER_CREATE_REGULATOR";
    private static final String ACTION_USER_ADMIN_UPDATE = "USER_ADMIN_UPDATE";
    private static final String ACTION_USER_SELF_UPDATE = "USER_SELF_UPDATE";
    private static final String ACTION_USER_PASSWORD_CHANGE = "USER_PASSWORD_CHANGE";
    private static final String ACTION_USER_DELETE = "USER_DELETE";
    private static final String ACTION_AUTH_LOGIN_SUCCESS = "AUTH_LOGIN_SUCCESS";
    private static final String ACTION_AUTH_LOGIN_FAILED = "AUTH_LOGIN_FAILED";
    private static final String ACTION_AUTH_LOGOUT_SUCCESS = "AUTH_LOGOUT_SUCCESS";
    private static final String ACTION_AUTH_PASSWORD_CHANGE_FAILED = "AUTH_PASSWORD_CHANGE_FAILED";
    private static final long LOGIN_IDENTIFIER_TARGET_ID = 0L;
    private static final List<String> ACCOUNT_AUDIT_ACTION_TYPES = List.of(
        ACTION_USER_REGISTER_PUBLIC,
        ACTION_USER_REGISTER_ENTERPRISE,
        ACTION_USER_CREATE_REGULATOR,
        ACTION_USER_SELF_UPDATE,
        ACTION_USER_PASSWORD_CHANGE
    );

    private final UserMapper userMapper;
    private final TokenUtil tokenUtil;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final AuthRedisService authRedisService;
    private final AuditLogService auditLogService;
    private final UserAuditSupport userAuditSupport;

    public UserServiceImpl(UserMapper userMapper,
                           TokenUtil tokenUtil,
                           RoleMapper roleMapper,
                           UserRoleMapper userRoleMapper,
                           AuthRedisService authRedisService,
                           AuditLogService auditLogService,
                           UserAuditSupport userAuditSupport) {
        this.userMapper = userMapper;
        this.tokenUtil = tokenUtil;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.authRedisService = authRedisService;
        this.auditLogService = auditLogService;
        this.userAuditSupport = userAuditSupport;
    }

    @Override
    public UserVO registerPublic(PublicRegisterDTO dto) {
        return register(buildRegisterDTO(dto, UserType.PUBLIC.code()), ACTION_USER_REGISTER_PUBLIC);
    }

    @Override
    public UserVO registerEnterprise(PublicRegisterDTO dto) {
        return register(buildRegisterDTO(dto, UserType.ENTERPRISE.code()), ACTION_USER_REGISTER_ENTERPRISE);
    }

    @Override
    public UserVO createRegulator(UserRegisterDTO dto) {
        dto.setUserType(UserType.REGULATOR.code());
        return register(dto, ACTION_USER_CREATE_REGULATOR);
    }

    private UserVO register(UserRegisterDTO dto, String forcedActionType) {
        String rawUserType = normalize(dto.getUserType());
        String roleCode = normalize(dto.getRoleCode());
        String userType = rawUserType;
        if (RoleCode.REGULATOR_ADMIN.code().equals(rawUserType)
            || RoleCode.REGULATOR_ENFORCER.code().equals(rawUserType)) {
            userType = UserType.REGULATOR.code();
            if (!StringUtils.hasText(roleCode)) {
                roleCode = rawUserType;
            }
        }
        if (!UserType.isValid(userType)) {
            throw new IllegalArgumentException("invalid user type");
        }
        String resolvedRoleCode = resolveRoleCode(userType, roleCode);
        User user = createBaseUser(dto.getUsername(), dto.getPassword(), dto.getRealName(), dto.getPhone(), userType);
        bindRoleByCode(user.getId(), resolvedRoleCode);
        List<String> roleCodes = loadRoleCodes(user.getId());
        RegistrationAuditAction auditAction = resolveRegistrationAuditAction(forcedActionType, userType, resolvedRoleCode);
        recordUserAudit(
            resolveRegistrationOperator(user, auditAction.selfRegistration()),
            user,
            auditAction.actionType(),
            auditAction.actionName(),
            "{}",
            userAuditSupport.writeUserSnapshot(user, roleCodes),
            auditAction.remark()
        );
        return toUserVO(user);
    }

    @Override
    public LoginResult login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw recordLoginFailure(dto, null, "invalid credentials");
        }
        if (!UserType.ADMIN.code().equals(user.getUserType())
            && !PasswordEncoderUtil.matches(dto.getPassword(), user.getPassword())) {
            throw recordLoginFailure(dto, user, "invalid credentials");
        }
        if (user.getStatus() != null && user.getStatus() == UserStatus.DISABLED.code()) {
            throw recordLoginFailure(dto, user, "user disabled");
        }
        if (user.getDeleted() != null && user.getDeleted() == 1) {
            throw recordLoginFailure(dto, user, "user deleted");
        }
        // 关键注释：登录时查询角色并写入 Token，减少网关与服务重复查库
        List<String> roles = loadRoleCodes(user.getId());
        String token = tokenUtil.generateToken(user.getId(), user.getUsername(), user.getUserType(), roles);
        cacheLoginSession(user, roles, token);
        recordAuthAudit(
            userAuditSupport.actorFromUser(user),
            AUDIT_TARGET_TYPE_USER,
            user.getId(),
            user.getId(),
            userAuditSupport.buildTargetName(user),
            ACTION_AUTH_LOGIN_SUCCESS,
            "\u767B\u5F55\u6210\u529F",
            1,
            null,
            "\u767B\u5F55\u6210\u529F"
        );
        LoginResult result = new LoginResult();
        result.setUserId(user.getId());
        result.setUsername(user.getUsername());
        result.setUserType(user.getUserType());
        result.setToken(token);
        result.setRoles(roles);
        return result;
    }

    @Override
    public void logout(String token) {
        User user = resolveUserFromToken(token);
        if (user != null) {
            recordAuthAudit(
                userAuditSupport.actorFromUser(user),
                AUDIT_TARGET_TYPE_USER,
                user.getId(),
                user.getId(),
                userAuditSupport.buildTargetName(user),
                ACTION_AUTH_LOGOUT_SUCCESS,
                "\u9000\u51FA\u767B\u5F55",
                1,
                null,
                "\u9000\u51FA\u767B\u5F55"
            );
        }
        if (StringUtils.hasText(token)) {
            String jti = tokenUtil.getJti(token);
            Long userId = tokenUtil.getUserId(token);
            long ttlSeconds = tokenUtil.getRemainingSeconds(token);
            String tokenHash = tokenUtil.getTokenHash(token);
            if (ttlSeconds > 0) {
                authRedisService.blacklist(jti, ttlSeconds);
            }
            authRedisService.deleteSession(jti);
            authRedisService.deleteIntrospect(tokenHash);
            authRedisService.unbindUserJti(userId, jti);
            tokenUtil.invalidate(token);
        }
    }

    @Override
    public UserVO getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || (user.getDeleted() != null && user.getDeleted() == 1)) {
            return null;
        }
        UserVO vo = toUserVO(user);
        // 关键注释：补齐角色信息，供网关/其他服务进行权限判断
        vo.setRoles(loadRoleCodes(user.getId()));
        return vo;
    }

    @Override
    public UserVO getCurrentUser(String token) {
        Long userId = requireUserId(token);
        return getUserById(userId);
    }

    @Override
    public UserVO updateUser(UserUpdateDTO dto) {
        User user = userMapper.selectById(dto.getId());
        if (user == null || (user.getDeleted() != null && user.getDeleted() == 1)) {
            throw new IllegalArgumentException("user not found");
        }
        List<String> roleCodes = loadRoleCodes(user.getId());
        String beforeData = userAuditSupport.writeUserSnapshot(user, roleCodes);
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setStatus(dto.getStatus());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        recordUserAudit(
            userAuditSupport.resolveCurrentOperator(),
            user,
            ACTION_USER_ADMIN_UPDATE,
            "\u66F4\u65B0\u7528\u6237\u4FE1\u606F",
            beforeData,
            userAuditSupport.writeUserSnapshot(user, roleCodes),
            "\u66F4\u65B0\u7528\u6237\u4FE1\u606F"
        );
        authRedisService.invalidateUserAllSessions(user.getId());
        return toUserVO(user);
    }

    @Override
    public UserVO updateCurrentUser(String token, UserSelfUpdateDTO dto) {
        User user = requireActiveUser(requireUserId(token));
        List<String> roleCodes = loadRoleCodes(user.getId());
        String beforeData = userAuditSupport.writeUserSnapshot(user, roleCodes);
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        recordUserAudit(
            resolveSelfOperator(user),
            user,
            ACTION_USER_SELF_UPDATE,
            "\u4FEE\u6539\u4E2A\u4EBA\u4FE1\u606F",
            beforeData,
            userAuditSupport.writeUserSnapshot(user, roleCodes),
            "\u4FEE\u6539\u4E2A\u4EBA\u4FE1\u606F"
        );
        UserVO vo = toUserVO(user);
        vo.setRoles(roleCodes);
        return vo;
    }

    @Override
    public void changeCurrentUserPassword(String token, UserPasswordChangeDTO dto) {
        User user = requireActiveUser(requireUserId(token));
        List<String> roleCodes = loadRoleCodes(user.getId());
        String beforeData = userAuditSupport.writeUserSnapshot(user, roleCodes);
        if (!PasswordEncoderUtil.matches(dto.getOldPassword(), user.getPassword())) {
            throw recordPasswordChangeFailure(user, "old password incorrect");
        }
        if (PasswordEncoderUtil.matches(dto.getNewPassword(), user.getPassword())) {
            throw recordPasswordChangeFailure(user, "new password must be different");
        }
        user.setPassword(PasswordEncoderUtil.encode(dto.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        recordUserAudit(
            resolveSelfOperator(user),
            user,
            ACTION_USER_PASSWORD_CHANGE,
            "\u4FEE\u6539\u5BC6\u7801",
            beforeData,
            userAuditSupport.writeUserSnapshot(user, roleCodes),
            "\u53D1\u751F\u4E86\u5BC6\u7801\u53D8\u66F4"
        );
        authRedisService.invalidateUserAllSessions(user.getId());
    }

    @Override
    public List<AuditLogVO> listCurrentUserAuditLogs(String token, int limit) {
        User user = requireActiveUser(requireUserId(token));
        return auditLogService.listTargetLogs(
            AUDIT_TARGET_TYPE_USER,
            user.getId(),
            ACCOUNT_AUDIT_ACTION_TYPES,
            limit
        );
    }

    @Override
    public void deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || (user.getDeleted() != null && user.getDeleted() == 1)) {
            return;
        }
        List<String> roleCodes = loadRoleCodes(id);
        String beforeData = userAuditSupport.writeUserSnapshot(user, roleCodes);
        user.setDeleted(1);
        user.setStatus(UserStatus.DISABLED.code());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        userRoleMapper.update(null, new LambdaUpdateWrapper<UserRole>()
            .eq(UserRole::getUserId, id)
            .set(UserRole::getDeleted, 1));
        recordUserAudit(
            userAuditSupport.resolveCurrentOperator(),
            user,
            ACTION_USER_DELETE,
            "\u5220\u9664\u7528\u6237",
            beforeData,
            userAuditSupport.writeUserSnapshot(user, List.of()),
            "\u5220\u9664\u7528\u6237"
        );
        authRedisService.invalidateUserAllSessions(id);
    }

    private void cacheLoginSession(User user, List<String> roles, String token) {
        if (user == null || !StringUtils.hasText(token)) {
            return;
        }
        String jti = tokenUtil.getJti(token);
        long ttlSeconds = tokenUtil.getRemainingSeconds(token);
        if (!StringUtils.hasText(jti) || ttlSeconds <= 0) {
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

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setUserType(user.getUserType());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }

    private UserRegisterDTO buildRegisterDTO(PublicRegisterDTO dto, String userType) {
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername(dto.getUsername());
        registerDTO.setPassword(dto.getPassword());
        registerDTO.setRealName(dto.getRealName());
        registerDTO.setPhone(dto.getPhone());
        registerDTO.setUserType(userType);
        return registerDTO;
    }

    private RegistrationAuditAction resolveRegistrationAuditAction(String forcedActionType,
                                                                  String userType,
                                                                  String resolvedRoleCode) {
        if (ACTION_USER_REGISTER_PUBLIC.equals(forcedActionType)) {
            return new RegistrationAuditAction(
                ACTION_USER_REGISTER_PUBLIC,
                "\u516C\u4F17\u7528\u6237\u6CE8\u518C",
                true,
                "\u516C\u4F17\u7528\u6237\u5B8C\u6210\u6CE8\u518C"
            );
        }
        if (ACTION_USER_REGISTER_ENTERPRISE.equals(forcedActionType)) {
            return new RegistrationAuditAction(
                ACTION_USER_REGISTER_ENTERPRISE,
                "\u4F01\u4E1A\u7528\u6237\u6CE8\u518C",
                true,
                "\u4F01\u4E1A\u7528\u6237\u5B8C\u6210\u6CE8\u518C"
            );
        }
        if (ACTION_USER_CREATE_REGULATOR.equals(forcedActionType)) {
            return new RegistrationAuditAction(
                ACTION_USER_CREATE_REGULATOR,
                "\u521B\u5EFA\u76D1\u7BA1\u8D26\u53F7",
                false,
                buildCreateRegulatorRemark(resolvedRoleCode)
            );
        }
        if (UserType.PUBLIC.code().equals(userType)) {
            return new RegistrationAuditAction(
                ACTION_USER_REGISTER_PUBLIC,
                "\u516C\u4F17\u7528\u6237\u6CE8\u518C",
                true,
                "\u516C\u4F17\u7528\u6237\u5B8C\u6210\u6CE8\u518C"
            );
        }
        if (UserType.ENTERPRISE.code().equals(userType)) {
            return new RegistrationAuditAction(
                ACTION_USER_REGISTER_ENTERPRISE,
                "\u4F01\u4E1A\u7528\u6237\u6CE8\u518C",
                true,
                "\u4F01\u4E1A\u7528\u6237\u5B8C\u6210\u6CE8\u518C"
            );
        }
        if (UserType.REGULATOR.code().equals(userType)) {
            return new RegistrationAuditAction(
                ACTION_USER_CREATE_REGULATOR,
                "\u521B\u5EFA\u76D1\u7BA1\u8D26\u53F7",
                false,
                buildCreateRegulatorRemark(resolvedRoleCode)
            );
        }
        return new RegistrationAuditAction(
            ACTION_USER_REGISTER,
            "\u6CE8\u518C\u7528\u6237",
            false,
            "\u5B8C\u6210\u7528\u6237\u6CE8\u518C"
        );
    }

    private UserAuditSupport.AuditActor resolveRegistrationOperator(User createdUser, boolean selfRegistration) {
        UserAuditSupport.AuditActor operator = userAuditSupport.resolveCurrentOperator();
        if (operator != null) {
            return operator;
        }
        return selfRegistration ? userAuditSupport.actorFromUser(createdUser) : null;
    }

    private UserAuditSupport.AuditActor resolveSelfOperator(User user) {
        UserAuditSupport.AuditActor operator = userAuditSupport.resolveCurrentOperator();
        return operator != null ? operator : userAuditSupport.actorFromUser(user);
    }

    private void recordUserAudit(UserAuditSupport.AuditActor operator,
                                 User targetUser,
                                 String actionType,
                                 String actionName,
                                 String beforeData,
                                 String afterData,
                                 String remark) {
        if (targetUser == null || targetUser.getId() == null) {
            return;
        }
        auditLogService.recordAudit(
            operator == null ? null : operator.userId(),
            operator == null ? null : operator.userType(),
            operator == null ? null : operator.operatorName(),
            AUDIT_TARGET_TYPE_USER,
            targetUser.getId(),
            targetUser.getId(),
            userAuditSupport.buildTargetName(targetUser),
            AUDIT_BIZ_TYPE_USER,
            actionType,
            actionName,
            beforeData,
            afterData,
            userAuditSupport.normalizeText(remark)
        );
    }

    private IllegalArgumentException recordLoginFailure(LoginDTO dto, User user, String message) {
        String username = dto == null ? null : userAuditSupport.normalizeText(dto.getUsername());
        UserAuditSupport.AuditActor actor = user == null
            ? new UserAuditSupport.AuditActor(null, null, username)
            : userAuditSupport.actorFromUser(user);
        String targetType = user == null ? AUDIT_TARGET_TYPE_LOGIN_IDENTIFIER : AUDIT_TARGET_TYPE_USER;
        Long targetId = user == null ? LOGIN_IDENTIFIER_TARGET_ID : user.getId();
        Long targetUserId = user == null ? null : user.getId();
        String targetName = user == null ? username : userAuditSupport.buildTargetName(user);
        recordAuthAudit(
            actor,
            targetType,
            targetId,
            targetUserId,
            targetName,
            ACTION_AUTH_LOGIN_FAILED,
            "\u767B\u5F55\u5931\u8D25",
            0,
            message,
            "\u767B\u5F55\u5931\u8D25"
        );
        return new IllegalArgumentException(message);
    }

    private IllegalArgumentException recordPasswordChangeFailure(User user, String message) {
        recordAuthAudit(
            userAuditSupport.actorFromUser(user),
            AUDIT_TARGET_TYPE_USER,
            user.getId(),
            user.getId(),
            userAuditSupport.buildTargetName(user),
            ACTION_AUTH_PASSWORD_CHANGE_FAILED,
            "\u4FEE\u6539\u5BC6\u7801\u5931\u8D25",
            0,
            message,
            "\u4FEE\u6539\u5BC6\u7801\u5931\u8D25"
        );
        return new IllegalArgumentException(message);
    }

    private void recordAuthAudit(UserAuditSupport.AuditActor actor,
                                 String targetType,
                                 Long targetId,
                                 Long targetUserId,
                                 String targetName,
                                 String actionType,
                                 String actionName,
                                 int successFlag,
                                 String errorMessage,
                                 String remark) {
        auditLogService.recordAudit(
            actor == null ? null : actor.userId(),
            actor == null ? null : actor.userType(),
            actor == null ? null : actor.operatorName(),
            targetType,
            targetId,
            targetUserId,
            targetName,
            AUDIT_BIZ_TYPE_AUTH,
            actionType,
            actionName,
            "{}",
            "{}",
            successFlag,
            errorMessage,
            remark
        );
    }

    private Long requireUserId(String token) {
        Long userId = tokenUtil.getUserId(token);
        if (userId == null) {
            throw new IllegalArgumentException("unauthorized");
        }
        return userId;
    }

    private User requireActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || Objects.equals(user.getDeleted(), 1)) {
            throw new IllegalArgumentException("user not found");
        }
        if (Objects.equals(user.getStatus(), UserStatus.DISABLED.code())) {
            throw new IllegalArgumentException("user disabled");
        }
        return user;
    }

    private User resolveUserFromToken(String token) {
        Long userId = tokenUtil.getUserId(token);
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        if (user == null || Objects.equals(user.getDeleted(), 1)) {
            return null;
        }
        return user;
    }

    private User createBaseUser(String username, String password, String realName, String phone, String userType) {
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, username));
        if (existing != null) {
            throw new IllegalArgumentException("username already exists");
        }
        if (!UserType.isValid(userType)) {
            throw new IllegalArgumentException("invalid user type");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordEncoderUtil.encode(password));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setUserType(userType);
        user.setStatus(UserStatus.ENABLED.code());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setDeleted(0);
        userMapper.insert(user);
        return user;
    }

    private void bindRoleByCode(Long userId, String roleCode) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
            .eq(Role::getRoleCode, roleCode));
        if (role == null) {
            throw new IllegalArgumentException("role not found for roleCode: " + roleCode);
        }
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
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

    private String resolveRoleCode(String userType, String roleCode) {
        if (UserType.REGULATOR.code().equals(userType)) {
            String resolved = StringUtils.hasText(roleCode) ? roleCode : RoleCode.REGULATOR_ENFORCER.code();
            if (!RoleCode.REGULATOR_ADMIN.code().equals(resolved)
                && !RoleCode.REGULATOR_ENFORCER.code().equals(resolved)) {
                throw new IllegalArgumentException("invalid regulator role");
            }
            return resolved;
        }
        return userType;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private String buildCreateRegulatorRemark(String roleCode) {
        String roleDisplayName = userAuditSupport.resolveRoleDisplayName(roleCode);
        if (StringUtils.hasText(roleDisplayName)) {
            return "\u521B\u5EFA\u76D1\u7BA1\u8D26\u53F7\uFF1A" + roleDisplayName;
        }
        return "\u521B\u5EFA\u76D1\u7BA1\u8D26\u53F7";
    }

    private record RegistrationAuditAction(String actionType,
                                           String actionName,
                                           boolean selfRegistration,
                                           String remark) {
    }
}
