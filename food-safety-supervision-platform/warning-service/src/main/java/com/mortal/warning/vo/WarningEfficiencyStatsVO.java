package com.mortal.warning.vo;

import lombok.Data;

/**
 * 预警处置效率统计。
 */
@Data
public class WarningEfficiencyStatsVO {
    private Long resolvedCount;
    private Long averageResolveMinutes;
    private Long pendingCount;
    private Long overduePendingCount;
    private Integer overdueHours;
}

