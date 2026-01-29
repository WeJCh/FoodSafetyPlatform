package com.mortal.user.service;

import com.mortal.user.vo.AuthIntrospectVO;

public interface AuthService {

    boolean verifyToken(String token);

    AuthIntrospectVO introspect(String token);
}
