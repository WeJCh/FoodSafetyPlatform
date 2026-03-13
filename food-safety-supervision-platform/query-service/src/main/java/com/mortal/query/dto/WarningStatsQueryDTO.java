package com.mortal.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 预警统计查询参数。
 */
@Data
public class WarningStatsQueryDTO {

    @Schema(
        description = "统计起始时间，格式：yyyy-MM-dd'T'HH:mm:ss（UTC+8 本地时间）",
        example = "2026-03-10T00:00:00"
    )
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(
        description = "统计结束时间，格式：yyyy-MM-dd'T'HH:mm:ss（UTC+8 本地时间）",
        example = "2026-03-13T23:59:59"
    )
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "预警类型精确过滤", example = "SLA_OVERDUE_SUBMIT")
    private String warningType;

    @Schema(description = "业务类型精确过滤", example = "RECTIFICATION")
    private String bizType;

    @Schema(description = "等级过滤（L1/L2）", example = "L1")
    private String level;

    @Schema(description = "状态过滤（OPEN/PROCESSING/RESOLVED/CLOSED）", example = "OPEN")
    private String status;

    @Schema(description = "辖区ID过滤", example = "330100")
    private Long regionId;

    @Schema(description = "辖区ID集合过滤（逗号分隔）", example = "330100,330101")
    private String regionIds;

    @Schema(description = "责任执法员ID过滤", example = "18")
    private Long ownerRegulatorId;

    @Schema(description = "类型统计TopN，默认5，最大20", example = "5")
    private Integer topN;

    @Schema(description = "趋势统计天数，默认7，最大60（仅未传起止时间时生效）", example = "7")
    private Integer trendDays;

    @Schema(description = "超时待处理阈值（小时），默认24", example = "24")
    private Integer overdueHours;
}
