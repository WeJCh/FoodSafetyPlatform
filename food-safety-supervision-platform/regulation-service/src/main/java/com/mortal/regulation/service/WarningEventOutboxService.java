package com.mortal.regulation.service;

import com.mortal.regulation.dto.WarningEventUpsertDTO;
import java.time.LocalDateTime;

/**
 * 预警事件 Outbox 服务。
 */
public interface WarningEventOutboxService {

    /**
     * 幂等写入（或补全）Outbox 待投递记录。
     */
    void ensurePendingEvent(String eventKey, WarningEventUpsertDTO dto, LocalDateTime now);

    /**
     * 按事件键尝试立即投递。
     *
     * @return true=已投递成功或已是成功态，false=暂未成功
     */
    boolean dispatchByEventKey(String eventKey);

    /**
     * 批量重试到期的待投递事件。
     *
     * @return 实际成功投递数量
     */
    int dispatchDueEvents(int batchSize);
}

