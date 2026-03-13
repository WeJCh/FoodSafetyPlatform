package com.mortal.warning.vo;

import lombok.Data;

/**
 * 统计分布项。
 */
@Data
public class WarningStatsItemVO {
    private String key;
    private String label;
    private Long count;
}

