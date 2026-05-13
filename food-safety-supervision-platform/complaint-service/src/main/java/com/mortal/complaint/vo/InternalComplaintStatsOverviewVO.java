package com.mortal.complaint.vo;

import lombok.Data;

/**
 * 投诉域概览统计。
 */
@Data
public class InternalComplaintStatsOverviewVO {

    private Long totalCount;

    private Long submittedCount;

    private Long pendingCount;

    private Long assignedCount;

    private Long processingCount;

    private Long feedbackedCount;

    private Long rejectedCount;

    private Long todoCount;

    private Long doneCount;

    private Long overdueCount;
}
