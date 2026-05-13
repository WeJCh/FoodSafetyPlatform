package com.mortal.user.service;

import com.mortal.user.dto.LoginDTO;
import com.mortal.user.dto.LoginResult;
import com.mortal.user.dto.PublicRegisterDTO;
import com.mortal.user.dto.UserPasswordChangeDTO;
import com.mortal.user.dto.UserRegisterDTO;
import com.mortal.user.dto.UserSelfUpdateDTO;
import com.mortal.user.dto.UserUpdateDTO;
import com.mortal.user.vo.UserVO;

public interface UserService {

    UserVO register(UserRegisterDTO dto);

    UserVO registerPublic(PublicRegisterDTO dto);

    LoginResult login(LoginDTO dto);

    void logout(String token);

    UserVO getUserById(Long id);

    UserVO getCurrentUser(String token);

    UserVO updateUser(UserUpdateDTO dto);

    UserVO updateCurrentUser(String token, UserSelfUpdateDTO dto);

    void changeCurrentUserPassword(String token, UserPasswordChangeDTO dto);

    void deleteUser(Long id);
}
