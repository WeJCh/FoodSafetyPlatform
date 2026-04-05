package com.mortal.query.vo;

import lombok.Data;

/**
 * 监管概览统计。
 */
@Data
public class SupervisionOverviewVO {

    private Long enterpriseTotalCount;

    private Long keyEnterpriseCount;

    private Long approvedEnterpriseCount;

    private Long inspectionTotalCount;

    private Long inspectionFailCount;

    private Long samplingTotalCount;

    private Long samplingFailCount;

    private Long complaintTotalCount;

    private Long complaintFeedbackedCount;

    private Long complaintOverdueCount;

    private Long openWarningCount;
}
