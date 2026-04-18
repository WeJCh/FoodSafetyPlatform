package com.mortal.query.support;

import com.mortal.platform.common.redis.PlatformRedisSupport;
import com.mortal.query.config.QueryRateLimitProperties;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class QueryRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(QueryRateLimitService.class);
    private static final String SCENE_WARNING_STATS = "query-warning-stats-rate-limit";
    private static final String SCENE_SUPERVISION_OVERVIEW = "query-supervision-overview-rate-limit";

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> fixedWindowRateLimitRedisScript;
    private final PlatformRedisSupport platformRedisSupport;
    private final QueryRateLimitProperties queryRateLimitProperties;

    public QueryRateLimitService(StringRedisTemplate stringRedisTemplate,
                                 DefaultRedisScript<Long> fixedWindowRateLimitRedisScript,
                                 PlatformRedisSupport platformRedisSupport,
                                 QueryRateLimitProperties queryRateLimitProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.fixedWindowRateLimitRedisScript = fixedWindowRateLimitRedisScript;
        this.platformRedisSupport = platformRedisSupport;
        this.queryRateLimitProperties = queryRateLimitProperties;
    }

    public boolean isWarningStatsAllowed(Long userId, String api) {
        QueryRateLimitProperties.WarningStats config = queryRateLimitProperties.getWarningStats();
        return isAllowed(
            config.isEnabled(),
            config.isFailOpen(),
            config.getWindowSeconds(),
            config.getMaxRequests(),
            SCENE_WARNING_STATS,
            platformRedisSupport.buildKey("rl", "query", "user", String.valueOf(userId), api)
        );
    }

    public boolean isSupervisionOverviewAllowed(Long userId) {
        QueryRateLimitProperties.SupervisionOverview config = queryRateLimitProperties.getSupervisionOverview();
        return isAllowed(
            config.isEnabled(),
            config.isFailOpen(),
            config.getWindowSeconds(),
            config.getMaxRequests(),
            SCENE_SUPERVISION_OVERVIEW,
            platformRedisSupport.buildKey("rl", "query", "user", String.valueOf(userId), "supervision-overview")
        );
    }

    private boolean isAllowed(boolean enabled,
                              boolean failOpen,
                              long windowSeconds,
                              long maxRequests,
                              String scene,
                              String key) {
        if (!enabled) {
            return true;
        }
        try {
            Long result = stringRedisTemplate.execute(
                fixedWindowRateLimitRedisScript,
                Collections.singletonList(key),
                String.valueOf(windowSeconds),
                String.valueOf(maxRequests)
            );
            platformRedisSupport.recordRecovery(scene);
            return Long.valueOf(1L).equals(result);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(scene, ex);
            if (failOpen) {
                log.error("Query rate limiter degraded to fail-open. scene={}", scene, ex);
                return true;
            }
            log.error("Query rate limiter rejected because Redis is unavailable. scene={}", scene, ex);
            return false;
        }
    }
}
