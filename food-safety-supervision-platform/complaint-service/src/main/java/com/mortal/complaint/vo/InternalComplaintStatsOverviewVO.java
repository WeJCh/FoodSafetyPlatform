package com.mortal.complaint.vo;

import lombok.Data;

/**
 * 投诉域概览统计。
 */
@Data
public class InternalComplaintStatsOverviewVO {

    private Long totalCount;

    private Long feedbackedCount;

    private Long overdueCount;
}
