package com.mortal.warning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

/**
 * 预警事件插入DTO
 */
@Data
public class WarningEventUpsertDTO {

    /**
     * 事件类型
     */
    @NotBlank(message = "eventType required")
    private String eventType;

    /**
     * 业务类型
     */
    @NotBlank(message = "bizType required")
    private String bizType;

    /**
     * 业务ID
     */
    @NotNull(message = "bizId required")
    private Long bizId;

    /**
     * 辖区ID（用于管理员权限过滤）
     */
    private Long regionId;

    /**
     * 责任执法员ID（用于执法员权限过滤）
     */
    private Long ownerRegulatorId;

    /**
     * 去重key
     */
    @NotBlank(message = "dedupKey required")
    @Size(max = 120, message = "dedupKey too long")
    private String dedupKey;

    /**
     * 预警级别
     */
    @NotBlank(message = "level required")
    private String level;

    /**
     * 预警标题
     */
    @Size(max = 100, message = "title too long")
    private String title;

    /**
     * 预警内容
     */
    @Size(max = 500, message = "content too long")
    private String content;

    /**
     * 预警来源
     */
    @Size(max = 50, message = "sourceService too long")
    private String sourceService;

    /**
     * 发生时间
     */
    private LocalDateTime occurTime;

    /**
     * 预警数据
     */
    private Map<String, Object> payload;
}
