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
    private String actionType;
    private Long operatorId;
    private String operatorName;
    private String actionComment;
    private LocalDateTime createTime;
}
