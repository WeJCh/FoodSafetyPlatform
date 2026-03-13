package com.mortal.warning.vo;

import lombok.Data;

/**
 * 预警类型统计项。
 */
@Data
public class WarningTypeStatsVO {
    private String warningType;
    private Long count;
}

