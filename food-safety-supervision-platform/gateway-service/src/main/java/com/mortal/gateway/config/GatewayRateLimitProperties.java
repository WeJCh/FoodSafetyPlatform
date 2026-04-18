package com.mortal.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gateway.rate-limit")
public class GatewayRateLimitProperties {

    private final Login login = new Login();

    public Login getLogin() {
        return login;
    }

    public static class Login {

        private boolean enabled = true;
        private boolean failOpen = true;
        private int ipWindowSeconds = 60;
        private int ipMaxRequests = 10;
        private int userWindowSeconds = 600;
        private int userMaxRequests = 5;

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

        public int getIpWindowSeconds() {
            return ipWindowSeconds;
        }

        public void setIpWindowSeconds(int ipWindowSeconds) {
            this.ipWindowSeconds = ipWindowSeconds;
        }

        public int getIpMaxRequests() {
            return ipMaxRequests;
        }

        public void setIpMaxRequests(int ipMaxRequests) {
            this.ipMaxRequests = ipMaxRequests;
        }

        public int getUserWindowSeconds() {
            return userWindowSeconds;
        }

        public void setUserWindowSeconds(int userWindowSeconds) {
            this.userWindowSeconds = userWindowSeconds;
        }

        public int getUserMaxRequests() {
            return userMaxRequests;
        }

        public void setUserMaxRequests(int userMaxRequests) {
            this.userMaxRequests = userMaxRequests;
        }
    }
}
