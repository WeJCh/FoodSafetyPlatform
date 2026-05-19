package com.mortal.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("audit_log")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String serviceName;
    private Long operatorUserId;
    private String operatorUserType;
    private String operatorName;
    private String targetType;
    private Long targetId;
    private Long targetUserId;
    private String targetName;
    private String bizType;
    private String actionType;
    private String actionName;
    private String beforeData;
    private String afterData;
    private Integer successFlag;
    private String errorMessage;
    private String remark;
    private String clientIp;
    private String traceId;
    private LocalDateTime createTime;
}
