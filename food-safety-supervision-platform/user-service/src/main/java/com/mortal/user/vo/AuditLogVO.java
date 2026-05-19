package com.mortal.user.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AuditLogVO {

    private Long id;
    private String bizType;
    private String targetType;
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
