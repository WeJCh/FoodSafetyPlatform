package com.mortal.regulation.support;

import com.mortal.platform.common.redis.PlatformRedisSupport;
import com.mortal.regulation.common.enums.FileBizType;
import com.mortal.regulation.config.RegulationRateLimitProperties;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class PresignRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(PresignRateLimitService.class);
    private static final String SCENE = "regulation-presign-rate-limit";

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> fixedWindowRateLimitRedisScript;
    private final PlatformRedisSupport platformRedisSupport;
    private final RegulationRateLimitProperties regulationRateLimitProperties;

    public PresignRateLimitService(StringRedisTemplate stringRedisTemplate,
                                   DefaultRedisScript<Long> fixedWindowRateLimitRedisScript,
                                   PlatformRedisSupport platformRedisSupport,
                                   RegulationRateLimitProperties regulationRateLimitProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.fixedWindowRateLimitRedisScript = fixedWindowRateLimitRedisScript;
        this.platformRedisSupport = platformRedisSupport;
        this.regulationRateLimitProperties = regulationRateLimitProperties;
    }

    public boolean isAllowed(Long userId, FileBizType bizType) {
        if (userId == null || bizType == null) {
            return false;
        }
        RegulationRateLimitProperties.Presign config = regulationRateLimitProperties.getPresign();
        if (!config.isEnabled()) {
            return true;
        }
        try {
            Long result = stringRedisTemplate.execute(
                fixedWindowRateLimitRedisScript,
                Collections.singletonList(buildKey(userId, bizType)),
                String.valueOf(config.getWindowSeconds()),
                String.valueOf(config.getMaxRequests())
            );
            platformRedisSupport.recordRecovery(SCENE);
            return Long.valueOf(1L).equals(result);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
            if (config.isFailOpen()) {
                log.error("File presign rate limiter degraded to fail-open. userId={}, bizType={}", userId, bizType, ex);
                return true;
            }
            log.error("File presign rate limiter rejected because Redis is unavailable. userId={}, bizType={}", userId, bizType, ex);
            return false;
        }
    }

    private String buildKey(Long userId, FileBizType bizType) {
        return platformRedisSupport.buildKey(
            "rl",
            "presign",
            "user",
            String.valueOf(userId),
            bizType.name().toLowerCase()
        );
    }
}
