package com.mortal.query.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "query.cache")
public class QueryCacheProperties {

    private final Scope scope = new Scope();
    private final SupervisionOverview supervisionOverview = new SupervisionOverview();

    public Scope getScope() {
        return scope;
    }

    public SupervisionOverview getSupervisionOverview() {
        return supervisionOverview;
    }

    public static class Scope {

        private boolean enabled = true;
        private long ttlSeconds = 600;
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

    public static class SupervisionOverview {

        private boolean enabled = true;
        private long ttlSeconds = 20;
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
