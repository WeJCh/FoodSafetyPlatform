package com.mortal.complaint.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "complaint.rate-limit")
public class ComplaintRateLimitProperties {

    private final PublicSubmit publicSubmit = new PublicSubmit();

    public PublicSubmit getPublicSubmit() {
        return publicSubmit;
    }

    public static class PublicSubmit {

        private boolean enabled = true;
        private boolean failOpen = true;
        private int windowSeconds = 600;
        private int maxRequests = 5;

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

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }

        public int getMaxRequests() {
            return maxRequests;
        }

        public void setMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
        }
    }
}
