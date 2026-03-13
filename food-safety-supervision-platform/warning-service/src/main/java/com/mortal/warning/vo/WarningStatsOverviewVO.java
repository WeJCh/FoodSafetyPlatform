package com.mortal.warning.vo;

import java.util.List;
import lombok.Data;

/**
 * 预警统计总览。
 */
@Data
public class WarningStatsOverviewVO {
    private Long totalCount;
    private Long openCount;
    private Long processingCount;
    private Long resolvedCount;
    private Long closedCount;
    /**
     * 已处理完成口径：RESOLVED + CLOSED。
     */
    private Long completedCount;
    private List<WarningStatsItemVO> statusDistribution;
    private List<WarningStatsItemVO> levelDistribution;
}
