package com.mortal.query.vo;

import lombok.Data;

/**
 * 企业统计概览。
 */
@Data
public class EnterpriseStatsOverviewVO {

    private Long totalCount;

    private Long keyEnterpriseCount;

    private Long approvedEnterpriseCount;
}
