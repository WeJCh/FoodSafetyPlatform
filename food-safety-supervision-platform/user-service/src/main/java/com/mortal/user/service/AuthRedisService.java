package com.mortal.user.service;

import com.mortal.user.vo.AuthIntrospectVO;
import java.util.Set;

public interface AuthRedisService {

    void saveSession(String jti, AuthSessionCacheValue session, long ttlSeconds);

    AuthSessionCacheValue getSession(String jti);

    void deleteSession(String jti);

    void blacklist(String jti, long ttlSeconds);

    boolean isBlacklisted(String jti);

    void bindUserJti(Long userId, String jti, long ttlSeconds);

    Set<String> getUserJtis(Long userId);

    void unbindUserJti(Long userId, String jti);

    void invalidateUserAllSessions(Long userId);

    void cacheIntrospect(String tokenHash, AuthIntrospectVO introspect, long ttlSeconds);

    AuthIntrospectVO getCachedIntrospect(String tokenHash);

    void deleteIntrospect(String tokenHash);
}
