package com.mortal.regulation.operation.service.scheduler;

import com.mortal.regulation.operation.config.OperationSchedulerLockProperties;
import com.mortal.regulation.operation.service.WarningEventOutboxService;
import com.mortal.regulation.operation.support.OperationSchedulerLockSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WarningEventOutboxRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(WarningEventOutboxRetryScheduler.class);
    private static final String SCHEDULER_LOCK_NAME = "reg-op:warning-outbox-retry";

    private final WarningEventOutboxService warningEventOutboxService;
    private final OperationSchedulerLockSupport operationSchedulerLockSupport;
    private final OperationSchedulerLockProperties operationSchedulerLockProperties;
    private final int batchSize;

    public WarningEventOutboxRetryScheduler(
        WarningEventOutboxService warningEventOutboxService,
        OperationSchedulerLockSupport operationSchedulerLockSupport,
        OperationSchedulerLockProperties operationSchedulerLockProperties,
        @Value("${regulation.warning.outbox.retry-batch-size:50}") int batchSize
    ) {
        this.warningEventOutboxService = warningEventOutboxService;
        this.operationSchedulerLockSupport = operationSchedulerLockSupport;
        this.operationSchedulerLockProperties = operationSchedulerLockProperties;
        this.batchSize = Math.max(1, batchSize);
    }

    @Scheduled(
        fixedDelayString = "${regulation.warning.outbox.retry-scan-ms:60000}",
        initialDelayString = "${regulation.warning.outbox.retry-initial-delay-ms:15000}"
    )
    public void retryPendingEvents() {
        try {
            boolean executed = operationSchedulerLockSupport.executeWithLock(
                SCHEDULER_LOCK_NAME,
                operationSchedulerLockProperties.getWarningOutboxRetryLeaseSeconds(),
                () -> {
                int successCount = warningEventOutboxService.dispatchDueEvents(batchSize);
                if (successCount > 0) {
                    log.info("Warning outbox retry sent {} event(s).", successCount);
                }
                }
            );
            if (!executed) {
                log.debug("Skip warning outbox retry because scheduler lock is held by another instance.");
            }
        } catch (Exception ex) {
            log.error("Warning outbox retry scan failed.", ex);
        }
    }
}
