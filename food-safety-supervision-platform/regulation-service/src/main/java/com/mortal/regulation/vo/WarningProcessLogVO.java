package com.mortal.regulation.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 预警处理日志视图。
 */
@Data
public class WarningProcessLogVO {

    private Long id;
    private Long warningId;
    private String warningNo;
    private String warningTitle;
    private String warningStatus;
    private String warningType;
    private String bizType;
    private Long bizId;
    private String bizName;
    private Long regionId;
    private String regionName;
    private String regionPathText;
    private Long ownerRegulatorId;
    private String ownerName;
    private Long assignedTo;
    private String assignedToName;
    private Long resolvedBy;
    private String resolvedByName;
    private String actionType;
    private Long operatorId;
    private String operatorName;
    private String actionComment;
    private LocalDateTime createTime;
}
