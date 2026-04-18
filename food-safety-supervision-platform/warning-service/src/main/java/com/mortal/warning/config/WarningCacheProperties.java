package com.mortal.warning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "warning.cache")
public class WarningCacheProperties {

    private final Stats stats = new Stats();

    public Stats getStats() {
        return stats;
    }

    public static class Stats {

        private boolean enabled = true;
        private long ttlSeconds = 30;
        private long lockLeaseSeconds = 5;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }

        public long getLockLeaseSeconds() {
            return lockLeaseSeconds;
        }

        public void setLockLeaseSeconds(long lockLeaseSeconds) {
            this.lockLeaseSeconds = lockLeaseSeconds;
        }
    }
}
