package com.mortal.complaint.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "complaint.lock")
public class ComplaintLockProperties {

    private long waitSeconds = 0;
    private long leaseSeconds = 15;

    public long getWaitSeconds() {
        return waitSeconds;
    }

    public void setWaitSeconds(long waitSeconds) {
        this.waitSeconds = waitSeconds;
    }

    public long getLeaseSeconds() {
        return leaseSeconds;
    }

    public void setLeaseSeconds(long leaseSeconds) {
        this.leaseSeconds = leaseSeconds;
    }
}
