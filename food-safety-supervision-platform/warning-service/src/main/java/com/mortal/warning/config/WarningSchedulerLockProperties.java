package com.mortal.warning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "warning.scheduler.lock")
public class WarningSchedulerLockProperties {

    private long waitSeconds = 0;
    private long escalationLeaseSeconds = 300;
    private long archiveLeaseSeconds = 900;

    public long getWaitSeconds() {
        return waitSeconds;
    }

    public void setWaitSeconds(long waitSeconds) {
        this.waitSeconds = waitSeconds;
    }

    public long getEscalationLeaseSeconds() {
        return escalationLeaseSeconds;
    }

    public void setEscalationLeaseSeconds(long escalationLeaseSeconds) {
        this.escalationLeaseSeconds = escalationLeaseSeconds;
    }

    public long getArchiveLeaseSeconds() {
        return archiveLeaseSeconds;
    }

    public void setArchiveLeaseSeconds(long archiveLeaseSeconds) {
        this.archiveLeaseSeconds = archiveLeaseSeconds;
    }
}
