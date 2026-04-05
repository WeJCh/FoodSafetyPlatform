package com.mortal.regulation.operation.vo;

import lombok.Data;

/**
 * 执行域概览统计。
 */
@Data
public class InternalOperationStatsOverviewVO {

    private Long inspectionTotalCount;

    private Long inspectionFailCount;

    private Long samplingTotalCount;

    private Long samplingFailCount;
}
