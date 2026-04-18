package com.mortal.complaint.support;

import com.mortal.complaint.config.ComplaintRateLimitProperties;
import com.mortal.platform.common.redis.PlatformRedisSupport;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class ComplaintRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintRateLimitService.class);
    private static final String SCENE = "complaint-public-submit-rate-limit";

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> fixedWindowRateLimitRedisScript;
    private final PlatformRedisSupport platformRedisSupport;
    private final ComplaintRateLimitProperties complaintRateLimitProperties;

    public ComplaintRateLimitService(StringRedisTemplate stringRedisTemplate,
                                     DefaultRedisScript<Long> fixedWindowRateLimitRedisScript,
                                     PlatformRedisSupport platformRedisSupport,
                                     ComplaintRateLimitProperties complaintRateLimitProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.fixedWindowRateLimitRedisScript = fixedWindowRateLimitRedisScript;
        this.platformRedisSupport = platformRedisSupport;
        this.complaintRateLimitProperties = complaintRateLimitProperties;
    }

    public boolean isPublicSubmitAllowed(Long userId) {
        if (userId == null) {
            return false;
        }
        ComplaintRateLimitProperties.PublicSubmit config = complaintRateLimitProperties.getPublicSubmit();
        if (!config.isEnabled()) {
            return true;
        }
        String key = publicSubmitKey(userId);
        try {
            Long result = stringRedisTemplate.execute(
                fixedWindowRateLimitRedisScript,
                Collections.singletonList(key),
                String.valueOf(config.getWindowSeconds()),
                String.valueOf(config.getMaxRequests())
            );
            platformRedisSupport.recordRecovery(SCENE);
            return Long.valueOf(1L).equals(result);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
            if (config.isFailOpen()) {
                log.error("Complaint public submit rate limiter degraded to fail-open. userId={}", userId, ex);
                return true;
            }
            log.error("Complaint public submit rate limiter rejected because Redis is unavailable. userId={}", userId, ex);
            return false;
        }
    }

    private String publicSubmitKey(Long userId) {
        return platformRedisSupport.buildKey(
            "rl",
            "complaint-submit",
            "user",
            String.valueOf(userId)
        );
    }
}
