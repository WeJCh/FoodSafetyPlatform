package com.mortal.regulation.vo.internal;

import lombok.Data;

/**
 * 企业主数据概览统计。
 */
@Data
public class InternalEnterpriseStatsOverviewVO {

    private Long totalCount;

    private Long keyEnterpriseCount;

    private Long approvedEnterpriseCount;
}
