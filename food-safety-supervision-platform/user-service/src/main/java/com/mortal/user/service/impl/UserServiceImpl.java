package com.mortal.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mortal.user.dto.LoginDTO;
import com.mortal.user.dto.LoginResult;
import com.mortal.user.dto.PublicRegisterDTO;
import com.mortal.user.dto.UserRegisterDTO;
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
import com.mortal.user.service.UserService;
import com.mortal.user.util.PasswordEncoderUtil;
import com.mortal.user.util.TokenUtil;
import com.mortal.user.vo.UserVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final TokenUtil tokenUtil;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public UserServiceImpl(UserMapper userMapper,
                           TokenUtil tokenUtil,
                           RoleMapper roleMapper,
                           UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.tokenUtil = tokenUtil;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public UserVO register(UserRegisterDTO dto) {
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
        User user = createBaseUser(dto.getUsername(), dto.getPassword(), dto.getRealName(), dto.getPhone(), userType);
        bindRoleByCode(user.getId(), resolveRoleCode(userType, roleCode));
        return toUserVO(user);
    }

    @Override
    public UserVO registerPublic(PublicRegisterDTO dto) {
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername(dto.getUsername());
        registerDTO.setPassword(dto.getPassword());
        registerDTO.setRealName(dto.getRealName());
        registerDTO.setPhone(dto.getPhone());
        registerDTO.setUserType(UserType.PUBLIC.code());
        return register(registerDTO);
    }

    @Override
    public LoginResult login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new IllegalArgumentException("invalid credentials");
        }
        if (!UserType.ADMIN.code().equals(user.getUserType())
            && !PasswordEncoderUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("invalid credentials");
        }
        if (user.getStatus() != null && user.getStatus() == UserStatus.DISABLED.code()) {
            throw new IllegalArgumentException("user disabled");
        }
        if (user.getDeleted() != null && user.getDeleted() == 1) {
            throw new IllegalArgumentException("user deleted");
        }
        // 关键注释：登录时查询角色并写入 Token，减少网关与服务重复查库
        List<String> roles = loadRoleCodes(user.getId());
        String token = tokenUtil.generateToken(user.getId(), user.getUsername(), user.getUserType(), roles);
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
        if (StringUtils.hasText(token)) {
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
    public UserVO updateUser(UserUpdateDTO dto) {
        User user = userMapper.selectById(dto.getId());
        if (user == null || (user.getDeleted() != null && user.getDeleted() == 1)) {
            throw new IllegalArgumentException("user not found");
        }
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setStatus(dto.getStatus());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return toUserVO(user);
    }

    @Override
    public void deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || (user.getDeleted() != null && user.getDeleted() == 1)) {
            return;
        }
        user.setDeleted(1);
        user.setStatus(UserStatus.DISABLED.code());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        userRoleMapper.update(null, new LambdaUpdateWrapper<UserRole>()
            .eq(UserRole::getUserId, id)
            .set(UserRole::getDeleted, 1));
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setUserType(user.getUserType());
        vo.setStatus(user.getStatus());
        return vo;
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
}
