package com.mortal.query.vo;

import lombok.Data;

/**
 * 投诉统计概览。
 */
@Data
public class ComplaintStatsOverviewVO {

    private Long totalCount;

    private Long feedbackedCount;

    private Long overdueCount;
}
