package com.mortal.platform.common.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.redis")
public class PlatformRedisProperties {

    /**
     * Unified business key prefix for Redis keys.
     */
    private String keyPrefix = "fsp";

    /**
     * Logical environment segment used in Redis keys.
     */
    private String env = "dev";

    /**
     * Global additional ttl jitter seconds for cache keys.
     */
    private long cacheTtlJitterSeconds = 30;

    /**
     * Cooldown window for repeated Redis unavailable alerts.
     */
    private long alertCooldownSeconds = 300;

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public long getCacheTtlJitterSeconds() {
        return cacheTtlJitterSeconds;
    }

    public void setCacheTtlJitterSeconds(long cacheTtlJitterSeconds) {
        this.cacheTtlJitterSeconds = cacheTtlJitterSeconds;
    }

    public long getAlertCooldownSeconds() {
        return alertCooldownSeconds;
    }

    public void setAlertCooldownSeconds(long alertCooldownSeconds) {
        this.alertCooldownSeconds = alertCooldownSeconds;
    }
}
