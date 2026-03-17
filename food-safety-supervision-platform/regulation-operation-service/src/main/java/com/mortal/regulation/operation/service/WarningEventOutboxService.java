package com.mortal.regulation.operation.service;

import com.mortal.regulation.operation.dto.WarningEventUpsertDTO;
import java.time.LocalDateTime;

public interface WarningEventOutboxService {

    void ensurePendingEvent(String eventKey, WarningEventUpsertDTO dto, LocalDateTime now);

    boolean dispatchByEventKey(String eventKey);

    int dispatchDueEvents(int batchSize);
}
