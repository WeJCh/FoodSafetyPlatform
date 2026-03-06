package com.mortal.regulation.dto;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

/**
 * 向 warning-service 上报预警事件的请求体。
 */
@Data
public class WarningEventUpsertDTO {

    private String eventType;
    private String bizType;
    private Long bizId;
    private Long regionId;
    private Long ownerRegulatorId;
    private String dedupKey;
    private String level;
    private String title;
    private String content;
    private String sourceService;
    private LocalDateTime occurTime;
    private Map<String, Object> payload;
}
