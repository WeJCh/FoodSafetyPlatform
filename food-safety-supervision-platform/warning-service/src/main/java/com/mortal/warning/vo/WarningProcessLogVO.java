package com.mortal.warning.vo;

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
    private Long regionId;
    private Long ownerRegulatorId;
    private Long assignedTo;
    private Long resolvedBy;
    private String actionType;
    private Long operatorId;
    private String operatorName;
    private String actionComment;
    private LocalDateTime createTime;
}
