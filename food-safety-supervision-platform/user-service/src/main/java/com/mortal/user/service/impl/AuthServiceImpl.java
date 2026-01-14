package com.mortal.user.service.impl;

import com.mortal.user.service.AuthService;
import com.mortal.user.util.TokenUtil;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final TokenUtil tokenUtil;

    public AuthServiceImpl(TokenUtil tokenUtil) {
        this.tokenUtil = tokenUtil;
    }

    @Override
    public boolean verifyToken(String token) {
        return tokenUtil.verify(token);
    }
}
