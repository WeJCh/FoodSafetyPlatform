package com.mortal.warning.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class WarningRecordVO {

    private Long id;
    private String warningNo;
    private String warningType;
    private String bizType;
    private Long bizId;
    private Long regionId;
    private Long ownerRegulatorId;
    private String dedupKey;
    private String level;
    private String status;
    private String title;
    private String content;
    private String sourceService;
    private LocalDateTime firstOccurTime;
    private LocalDateTime lastOccurTime;
    private Integer triggerCount;
    private Long assignedTo;
    private LocalDateTime assignedTime;
    private Long resolvedBy;
    private LocalDateTime resolvedTime;
    private String closeReason;
    private String payloadJson;
}
