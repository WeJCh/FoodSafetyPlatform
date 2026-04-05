package com.mortal.query.vo;

import lombok.Data;

/**
 * 执行域统计概览。
 */
@Data
public class OperationStatsOverviewVO {

    private Long inspectionTotalCount;

    private Long inspectionFailCount;

    private Long samplingTotalCount;

    private Long samplingFailCount;
}
