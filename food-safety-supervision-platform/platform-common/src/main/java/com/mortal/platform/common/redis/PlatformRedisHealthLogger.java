package com.mortal.platform.common.redis;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class PlatformRedisHealthLogger {

    private static final Logger log = LoggerFactory.getLogger(PlatformRedisHealthLogger.class);

    private final PlatformRedisProperties platformRedisProperties;
    private final ConcurrentMap<String, RedisHealthState> states = new ConcurrentHashMap<>();

    public PlatformRedisHealthLogger(PlatformRedisProperties platformRedisProperties) {
        this.platformRedisProperties = platformRedisProperties;
    }

    public void recordFailure(String scene, Exception ex) {
        String normalizedScene = normalizeScene(scene);
        RedisHealthState state = states.computeIfAbsent(normalizedScene, key -> new RedisHealthState());
        long now = Instant.now().toEpochMilli();
        long cooldownMillis = Math.max(1L, platformRedisProperties.getAlertCooldownSeconds()) * 1000L;
        boolean shouldLog = !state.unavailable || now - state.lastFailureLogAt >= cooldownMillis;
        state.unavailable = true;
        if (shouldLog) {
            state.lastFailureLogAt = now;
            log.error("Redis unavailable, fallback/degrade activated. scene={}", normalizedScene, ex);
        }
    }

    public void recordRecovery(String scene) {
        String normalizedScene = normalizeScene(scene);
        RedisHealthState state = states.computeIfAbsent(normalizedScene, key -> new RedisHealthState());
        if (state.unavailable) {
            state.unavailable = false;
            state.lastFailureLogAt = 0L;
            log.info("Redis access recovered. scene={}", normalizedScene);
        }
    }

    private String normalizeScene(String scene) {
        if (scene == null || scene.isBlank()) {
            return "unknown";
        }
        return scene.trim();
    }

    private static final class RedisHealthState {
        private volatile boolean unavailable;
        private volatile long lastFailureLogAt;
    }
}
