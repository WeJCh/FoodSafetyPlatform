package com.mortal.regulation.operation.dto;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

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
