package com.mortal.regulation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "regulation.cache")
public class RegulationCacheProperties {

    private final Region region = new Region();
    private final MasterData masterData = new MasterData();
    private final PublicData publicData = new PublicData();

    public Region getRegion() {
        return region;
    }

    public MasterData getMasterData() {
        return masterData;
    }

    public PublicData getPublicData() {
        return publicData;
    }

    public static class Region {

        private boolean enabled = true;
        private long ttlSeconds = 86400;
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

    public static class MasterData {

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

    public static class PublicData {

        private boolean enabled = true;
        private long listTtlSeconds = 60;
        private long detailTtlSeconds = 300;
        private long lockLeaseSeconds = 5;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getListTtlSeconds() {
            return listTtlSeconds;
        }

        public void setListTtlSeconds(long listTtlSeconds) {
            this.listTtlSeconds = listTtlSeconds;
        }

        public long getDetailTtlSeconds() {
            return detailTtlSeconds;
        }

        public void setDetailTtlSeconds(long detailTtlSeconds) {
            this.detailTtlSeconds = detailTtlSeconds;
        }

        public long getLockLeaseSeconds() {
            return lockLeaseSeconds;
        }

        public void setLockLeaseSeconds(long lockLeaseSeconds) {
            this.lockLeaseSeconds = lockLeaseSeconds;
        }
    }
}
