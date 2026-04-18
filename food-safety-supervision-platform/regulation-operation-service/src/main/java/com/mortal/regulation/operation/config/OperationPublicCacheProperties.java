package com.mortal.regulation.operation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "regulation.cache.public-data")
public class OperationPublicCacheProperties {

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
