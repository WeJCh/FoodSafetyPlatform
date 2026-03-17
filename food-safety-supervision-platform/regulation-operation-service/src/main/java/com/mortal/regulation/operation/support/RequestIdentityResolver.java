package com.mortal.regulation.operation.support;

import com.mortal.regulation.operation.common.OperationErrorMessages;
import com.mortal.regulation.operation.common.RequestIdentity;
import com.mortal.regulation.operation.util.JwtUserResolver;
import org.springframework.stereotype.Component;

@Component
public class RequestIdentityResolver {

    private final JwtUserResolver jwtUserResolver;

    public RequestIdentityResolver(JwtUserResolver jwtUserResolver) {
        this.jwtUserResolver = jwtUserResolver;
    }

    public RequestIdentity resolve(String token) {
        Long userId = jwtUserResolver.resolveUserId(token);
        if (userId == null) {
            throw new IllegalArgumentException(OperationErrorMessages.UNAUTHORIZED);
        }
        return new RequestIdentity(userId, jwtUserResolver.resolveUserType(token));
    }
}
