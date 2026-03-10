package com.mortal.regulation.service.scheduler;

import com.mortal.regulation.service.WarningEventOutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Outbox 事件重试调度器。
 */
@Component
public class WarningEventOutboxRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(WarningEventOutboxRetryScheduler.class);

    private final WarningEventOutboxService warningEventOutboxService;
    private final int batchSize;

    public WarningEventOutboxRetryScheduler(
        WarningEventOutboxService warningEventOutboxService,
        @Value("${regulation.warning.outbox.retry-batch-size:50}") int batchSize
    ) {
        this.warningEventOutboxService = warningEventOutboxService;
        this.batchSize = Math.max(1, batchSize);
    }

    /**
     * 定时重试 warning 事件投递，保障下游短暂不可用时最终可达。
     */
    @Scheduled(
        fixedDelayString = "${regulation.warning.outbox.retry-scan-ms:60000}",
        initialDelayString = "${regulation.warning.outbox.retry-initial-delay-ms:15000}"
    )
    public void retryPendingEvents() {
        try {
            int successCount = warningEventOutboxService.dispatchDueEvents(batchSize);
            if (successCount > 0) {
                log.info("Warning outbox retry sent {} event(s).", successCount);
            }
        } catch (Exception ex) {
            log.error("Warning outbox retry scan failed.", ex);
        }
    }
}

