package com.mortal.warning.dto;

import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 预警统计查询参数。
 */
@Data
public class WarningStatsQueryDTO {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    private String warningType;
    private String bizType;
    private String level;
    private String status;

    /**
     * 按辖区过滤（可选）。
     */
    private Long regionId;

    /**
     * 按辖区集合过滤（逗号分隔，可选）。
     */
    private String regionIds;

    /**
     * 按分派处理人过滤（执法员统计，可选）。
     */
    private Long assignedTo;

    /**
     * 类型统计 TopN，默认 5。
     */
    private Integer topN;

    /**
     * 趋势天数，未传起止时间时生效，默认 7。
     */
    private Integer trendDays;

    /**
     * 待处理超时阈值（小时），默认 24。
     */
    private Integer overdueHours;
}
