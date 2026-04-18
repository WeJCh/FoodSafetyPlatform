package com.mortal.query.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "query.rate-limit")
public class QueryRateLimitProperties {

    private final WarningStats warningStats = new WarningStats();
    private final SupervisionOverview supervisionOverview = new SupervisionOverview();

    public WarningStats getWarningStats() {
        return warningStats;
    }

    public SupervisionOverview getSupervisionOverview() {
        return supervisionOverview;
    }

    public static class WarningStats {
        private boolean enabled = true;
        private boolean failOpen = true;
        private long windowSeconds = 60;
        private long maxRequests = 120;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isFailOpen() {
            return failOpen;
        }

        public void setFailOpen(boolean failOpen) {
            this.failOpen = failOpen;
        }

        public long getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(long windowSeconds) {
            this.windowSeconds = windowSeconds;
        }

        public long getMaxRequests() {
            return maxRequests;
        }

        public void setMaxRequests(long maxRequests) {
            this.maxRequests = maxRequests;
        }
    }

    public static class SupervisionOverview {
        private boolean enabled = true;
        private boolean failOpen = true;
        private long windowSeconds = 60;
        private long maxRequests = 120;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isFailOpen() {
            return failOpen;
        }

        public void setFailOpen(boolean failOpen) {
            this.failOpen = failOpen;
        }

        public long getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(long windowSeconds) {
            this.windowSeconds = windowSeconds;
        }

        public long getMaxRequests() {
            return maxRequests;
        }

        public void setMaxRequests(long maxRequests) {
            this.maxRequests = maxRequests;
        }
    }
}
