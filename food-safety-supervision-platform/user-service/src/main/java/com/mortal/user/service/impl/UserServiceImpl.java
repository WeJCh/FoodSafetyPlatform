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
import com.mortal.user.mapper.RoleMapper;
import com.mortal.user.mapper.UserMapper;
import com.mortal.user.mapper.UserRoleMapper;
import com.mortal.user.service.UserService;
import com.mortal.user.util.PasswordEncoderUtil;
import com.mortal.user.util.TokenUtil;
import com.mortal.user.vo.UserVO;
import java.time.LocalDateTime;
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
        if ("REGULATOR_ADMIN".equals(rawUserType) || "REGULATOR_ENFORCER".equals(rawUserType)) {
            userType = "REGULATOR";
            if (!StringUtils.hasText(roleCode)) {
                roleCode = rawUserType;
            }
        }
        if (!"PUBLIC".equals(userType) && !"ENTERPRISE".equals(userType) && !"REGULATOR".equals(userType)) {
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
        registerDTO.setUserType("PUBLIC");
        return register(registerDTO);
    }

    @Override
    public LoginResult login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new IllegalArgumentException("invalid credentials");
        }
        if (!"ADMIN".equals(user.getUserType())
            && !PasswordEncoderUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("invalid credentials");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new IllegalArgumentException("user disabled");
        }
        if (user.getDeleted() != null && user.getDeleted() == 1) {
            throw new IllegalArgumentException("user deleted");
        }
        String token = tokenUtil.generateToken(user.getId(), user.getUsername(), user.getUserType());
        LoginResult result = new LoginResult();
        result.setUserId(user.getId());
        result.setUsername(user.getUsername());
        result.setUserType(user.getUserType());
        result.setToken(token);
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
        return toUserVO(user);
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
        user.setStatus(0);
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
        if (!"PUBLIC".equals(userType) && !"ENTERPRISE".equals(userType) && !"REGULATOR".equals(userType)) {
            throw new IllegalArgumentException("invalid user type");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordEncoderUtil.encode(password));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setUserType(userType);
        user.setStatus(1);
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

    private String resolveRoleCode(String userType, String roleCode) {
        if ("REGULATOR".equals(userType)) {
            String resolved = StringUtils.hasText(roleCode) ? roleCode : "REGULATOR_ENFORCER";
            if (!"REGULATOR_ADMIN".equals(resolved) && !"REGULATOR_ENFORCER".equals(resolved)) {
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
