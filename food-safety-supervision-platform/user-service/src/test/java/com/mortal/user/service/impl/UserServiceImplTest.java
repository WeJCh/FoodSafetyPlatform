package com.mortal.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mortal.user.entity.User;
import com.mortal.user.enums.UserStatus;
import com.mortal.user.mapper.RoleMapper;
import com.mortal.user.mapper.UserMapper;
import com.mortal.user.mapper.UserRoleMapper;
import com.mortal.user.service.AuditLogService;
import com.mortal.user.service.AuthRedisService;
import com.mortal.user.support.UserAuditSupport;
import com.mortal.user.util.TokenUtil;
import com.mortal.user.vo.AuditLogVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private TokenUtil tokenUtil;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private AuthRedisService authRedisService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private UserAuditSupport userAuditSupport;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
            userMapper,
            tokenUtil,
            roleMapper,
            userRoleMapper,
            authRedisService,
            auditLogService,
            userAuditSupport
        );
    }

    @Test
    void listCurrentUserAuditLogs_shouldUseAccountWhitelistToAvoidRegulatorDuplicateRecords() {
        User regulator = new User();
        regulator.setId(8L);
        regulator.setUsername("regulator_admin");
        regulator.setStatus(UserStatus.ENABLED.code());
        regulator.setDeleted(0);

        AuditLogVO log = new AuditLogVO();
        log.setActionType("USER_SELF_UPDATE");
        List<AuditLogVO> expectedLogs = List.of(log);

        when(tokenUtil.getUserId("Bearer test-token")).thenReturn(8L);
        when(userMapper.selectById(8L)).thenReturn(regulator);
        when(auditLogService.listTargetLogs(eq("USER"), eq(8L), anyList(), eq(6))).thenReturn(expectedLogs);

        List<AuditLogVO> actualLogs = userService.listCurrentUserAuditLogs("Bearer test-token", 6);

        assertThat(actualLogs).isSameAs(expectedLogs);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> actionTypesCaptor = ArgumentCaptor.forClass(List.class);
        verify(auditLogService).listTargetLogs(eq("USER"), eq(8L), actionTypesCaptor.capture(), eq(6));

        assertThat(actionTypesCaptor.getValue()).containsExactly(
            "USER_REGISTER_PUBLIC",
            "USER_REGISTER_ENTERPRISE",
            "USER_CREATE_REGULATOR",
            "USER_SELF_UPDATE",
            "USER_PASSWORD_CHANGE"
        );
        assertThat(actionTypesCaptor.getValue()).doesNotContain(
            "USER_ADMIN_UPDATE",
            "USER_ROLE_BIND",
            "AUTH_LOGIN_SUCCESS",
            "AUTH_LOGIN_FAILED",
            "AUTH_LOGOUT_SUCCESS",
            "AUTH_PASSWORD_CHANGE_FAILED"
        );
    }
}
