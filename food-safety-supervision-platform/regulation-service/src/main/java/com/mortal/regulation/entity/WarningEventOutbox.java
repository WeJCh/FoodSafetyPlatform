package com.mortal.regulation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 预警事件 Outbox 记录。
 */
@Data
@TableName("warning_event_outbox")
public class WarningEventOutbox {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventKey;
    private String eventType;
    private String payloadJson;
    private String status;
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private LocalDateTime lastAttemptTime;
    private String lastError;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}

