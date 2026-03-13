package com.mortal.query.vo;

import java.util.List;
import lombok.Data;

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
