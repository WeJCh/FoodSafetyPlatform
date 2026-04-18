package com.mortal.user.service.impl;

import com.mortal.platform.common.redis.PlatformRedisSupport;
import com.mortal.user.service.AuthRedisService;
import com.mortal.user.service.AuthSessionCacheValue;
import com.mortal.user.vo.AuthIntrospectVO;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthRedisServiceImpl implements AuthRedisService {

    private static final String DOMAIN_AUTH = "auth";
    private static final String BLACKLIST = "blacklist";
    private static final String SESSION = "session";
    private static final String USER_JTIS = "user-jtis";
    private static final String INTROSPECT = "introspect";
    private static final String SCENE = "user-auth-cache";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PlatformRedisSupport platformRedisSupport;

    public AuthRedisServiceImpl(StringRedisTemplate stringRedisTemplate,
                                RedisTemplate<String, Object> redisTemplate,
                                PlatformRedisSupport platformRedisSupport) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisTemplate = redisTemplate;
        this.platformRedisSupport = platformRedisSupport;
    }

    @Override
    public void saveSession(String jti, AuthSessionCacheValue session, long ttlSeconds) {
        if (!StringUtils.hasText(jti) || session == null || ttlSeconds <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(sessionKey(jti), session, platformRedisSupport.fixedTtl(ttlSeconds));
            platformRedisSupport.recordRecovery(SCENE);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
        }
    }

    @Override
    public AuthSessionCacheValue getSession(String jti) {
        if (!StringUtils.hasText(jti)) {
            return null;
        }
        try {
            Object value = redisTemplate.opsForValue().get(sessionKey(jti));
            platformRedisSupport.recordRecovery(SCENE);
            if (value instanceof AuthSessionCacheValue session) {
                return session;
            }
            return null;
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
            return null;
        }
    }

    @Override
    public void deleteSession(String jti) {
        if (!StringUtils.hasText(jti)) {
            return;
        }
        try {
            redisTemplate.delete(sessionKey(jti));
            platformRedisSupport.recordRecovery(SCENE);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
        }
    }

    @Override
    public void blacklist(String jti, long ttlSeconds) {
        if (!StringUtils.hasText(jti) || ttlSeconds <= 0) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(blacklistKey(jti), "1", platformRedisSupport.fixedTtl(ttlSeconds));
            platformRedisSupport.recordRecovery(SCENE);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
        }
    }

    @Override
    public boolean isBlacklisted(String jti) {
        if (!StringUtils.hasText(jti)) {
            return false;
        }
        try {
            Boolean result = stringRedisTemplate.hasKey(blacklistKey(jti));
            platformRedisSupport.recordRecovery(SCENE);
            return Boolean.TRUE.equals(result);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
            return false;
        }
    }

    @Override
    public void bindUserJti(Long userId, String jti, long ttlSeconds) {
        if (userId == null || !StringUtils.hasText(jti)) {
            return;
        }
        String key = userJtisKey(userId);
        try {
            stringRedisTemplate.opsForSet().add(key, jti);
            if (ttlSeconds > 0) {
                stringRedisTemplate.expire(key, platformRedisSupport.fixedTtl(ttlSeconds));
            }
            platformRedisSupport.recordRecovery(SCENE);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
        }
    }

    @Override
    public Set<String> getUserJtis(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        Set<String> members;
        try {
            members = stringRedisTemplate.opsForSet().members(userJtisKey(userId));
            platformRedisSupport.recordRecovery(SCENE);
            if (members == null || members.isEmpty()) {
                return Collections.emptySet();
            }
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String member : members) {
            if (StringUtils.hasText(member)) {
                result.add(member);
            }
        }
        return result;
    }

    @Override
    public void unbindUserJti(Long userId, String jti) {
        if (userId == null || !StringUtils.hasText(jti)) {
            return;
        }
        try {
            stringRedisTemplate.opsForSet().remove(userJtisKey(userId), jti);
            platformRedisSupport.recordRecovery(SCENE);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
        }
    }

    @Override
    public void invalidateUserAllSessions(Long userId) {
        if (userId == null) {
            return;
        }
        Set<String> jtis = getUserJtis(userId);
        for (String jti : jtis) {
            String sessionKey = sessionKey(jti);
            Long ttlSeconds;
            try {
                ttlSeconds = redisTemplate.getExpire(sessionKey);
                platformRedisSupport.recordRecovery(SCENE);
            } catch (Exception ex) {
                platformRedisSupport.recordFailure(SCENE, ex);
                ttlSeconds = null;
            }
            if (ttlSeconds != null && ttlSeconds > 0) {
                blacklist(jti, ttlSeconds);
            }
            deleteSession(jti);
        }
        try {
            redisTemplate.delete(userJtisKey(userId));
            platformRedisSupport.recordRecovery(SCENE);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
        }
    }

    @Override
    public void cacheIntrospect(String tokenHash, AuthIntrospectVO introspect, long ttlSeconds) {
        if (!StringUtils.hasText(tokenHash) || introspect == null || ttlSeconds <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(introspectKey(tokenHash), introspect, platformRedisSupport.fixedTtl(ttlSeconds));
            platformRedisSupport.recordRecovery(SCENE);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
        }
    }

    @Override
    public AuthIntrospectVO getCachedIntrospect(String tokenHash) {
        if (!StringUtils.hasText(tokenHash)) {
            return null;
        }
        try {
            Object value = redisTemplate.opsForValue().get(introspectKey(tokenHash));
            platformRedisSupport.recordRecovery(SCENE);
            if (value instanceof AuthIntrospectVO introspect) {
                return introspect;
            }
            return null;
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
            return null;
        }
    }

    @Override
    public void deleteIntrospect(String tokenHash) {
        if (!StringUtils.hasText(tokenHash)) {
            return;
        }
        try {
            redisTemplate.delete(introspectKey(tokenHash));
            platformRedisSupport.recordRecovery(SCENE);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
        }
    }

    private String blacklistKey(String jti) {
        return key(DOMAIN_AUTH, BLACKLIST, jti);
    }

    private String sessionKey(String jti) {
        return key(DOMAIN_AUTH, SESSION, jti);
    }

    private String userJtisKey(Long userId) {
        return key(DOMAIN_AUTH, USER_JTIS, String.valueOf(userId));
    }

    private String introspectKey(String tokenHash) {
        return key(DOMAIN_AUTH, INTROSPECT, tokenHash);
    }

    private String key(String domain, String biz, String id) {
        return platformRedisSupport.buildKey(domain, biz, id);
    }
}
