package com.mortal.platform.common.redis;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.util.StringUtils;

public class PlatformRedisSupport {

    private final PlatformRedisProperties platformRedisProperties;
    private final PlatformRedisHealthLogger platformRedisHealthLogger;

    public PlatformRedisSupport(PlatformRedisProperties platformRedisProperties,
                                PlatformRedisHealthLogger platformRedisHealthLogger) {
        this.platformRedisProperties = platformRedisProperties;
        this.platformRedisHealthLogger = platformRedisHealthLogger;
    }

    public String buildKey(String... segments) {
        String[] keys = new String[segments.length + 2];
        keys[0] = normalize(platformRedisProperties.getKeyPrefix(), "fsp");
        keys[1] = normalize(platformRedisProperties.getEnv(), "dev");
        System.arraycopy(segments, 0, keys, 2, segments.length);
        return String.join(":", keys);
    }

    public Duration jitterTtl(long ttlSeconds) {
        return Duration.ofSeconds(jitterTtlSeconds(ttlSeconds));
    }

    public Duration fixedTtl(long ttlSeconds) {
        return Duration.ofSeconds(Math.max(1L, ttlSeconds));
    }

    public long jitterTtlSeconds(long ttlSeconds) {
        long safeTtlSeconds = Math.max(1L, ttlSeconds);
        long maxJitter = Math.max(0L, platformRedisProperties.getCacheTtlJitterSeconds());
        if (maxJitter <= 0L || safeTtlSeconds <= 1L) {
            return safeTtlSeconds;
        }
        long boundedJitter = Math.min(maxJitter, Math.max(1L, safeTtlSeconds / 10L));
        return safeTtlSeconds + ThreadLocalRandom.current().nextLong(boundedJitter + 1L);
    }

    public void recordFailure(String scene, Exception ex) {
        platformRedisHealthLogger.recordFailure(scene, ex);
    }

    public void recordRecovery(String scene) {
        platformRedisHealthLogger.recordRecovery(scene);
    }

    private String normalize(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
