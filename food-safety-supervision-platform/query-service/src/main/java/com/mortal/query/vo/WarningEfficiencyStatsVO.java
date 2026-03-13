package com.mortal.query.vo;

import lombok.Data;

@Data
public class WarningEfficiencyStatsVO {
    private Long resolvedCount;
    private Long averageResolveMinutes;
    private Long pendingCount;
    private Long overduePendingCount;
    private Integer overdueHours;
}

