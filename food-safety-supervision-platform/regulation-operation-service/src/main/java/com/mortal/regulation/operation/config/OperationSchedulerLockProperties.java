package com.mortal.regulation.operation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "operation.scheduler.lock")
public class OperationSchedulerLockProperties {

    private long waitSeconds = 0;
    private long rectificationSlaLeaseSeconds = 480;
    private long warningOutboxRetryLeaseSeconds = 60;

    public long getWaitSeconds() {
        return waitSeconds;
    }

    public void setWaitSeconds(long waitSeconds) {
        this.waitSeconds = waitSeconds;
    }

    public long getRectificationSlaLeaseSeconds() {
        return rectificationSlaLeaseSeconds;
    }

    public void setRectificationSlaLeaseSeconds(long rectificationSlaLeaseSeconds) {
        this.rectificationSlaLeaseSeconds = rectificationSlaLeaseSeconds;
    }

    public long getWarningOutboxRetryLeaseSeconds() {
        return warningOutboxRetryLeaseSeconds;
    }

    public void setWarningOutboxRetryLeaseSeconds(long warningOutboxRetryLeaseSeconds) {
        this.warningOutboxRetryLeaseSeconds = warningOutboxRetryLeaseSeconds;
    }
}
