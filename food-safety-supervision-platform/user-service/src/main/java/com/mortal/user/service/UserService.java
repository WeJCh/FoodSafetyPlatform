package com.mortal.user.service;

import com.mortal.user.dto.LoginDTO;
import com.mortal.user.dto.LoginResult;
import com.mortal.user.dto.PublicRegisterDTO;
import com.mortal.user.dto.UserPasswordChangeDTO;
import com.mortal.user.dto.UserRegisterDTO;
import com.mortal.user.dto.UserSelfUpdateDTO;
import com.mortal.user.dto.UserUpdateDTO;
import com.mortal.user.vo.AuditLogVO;
import com.mortal.user.vo.UserVO;
import java.util.List;

public interface UserService {

    UserVO registerPublic(PublicRegisterDTO dto);

    UserVO registerEnterprise(PublicRegisterDTO dto);

    UserVO createRegulator(UserRegisterDTO dto);

    LoginResult login(LoginDTO dto);

    void logout(String token);

    UserVO getUserById(Long id);

    UserVO getCurrentUser(String token);

    UserVO updateUser(UserUpdateDTO dto);

    UserVO updateCurrentUser(String token, UserSelfUpdateDTO dto);

    void changeCurrentUserPassword(String token, UserPasswordChangeDTO dto);

    List<AuditLogVO> listCurrentUserAuditLogs(String token, int limit);

    void deleteUser(Long id);
}
