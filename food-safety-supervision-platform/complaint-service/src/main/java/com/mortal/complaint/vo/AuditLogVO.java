package com.mortal.complaint.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 审计日志视图对象
 */
@Data
public class AuditLogVO {

    private Long id;
    private String actionType;
    private String actionName;
    private String operatorName;
    private Long targetId;
    private Long targetUserId;
    private String targetName;
    private String summary;
    private String remark;
    private String beforeData;
    private String afterData;
    private LocalDateTime createTime;
}
