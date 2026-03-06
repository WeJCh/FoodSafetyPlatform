package com.mortal.warning.vo;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 预警详情视图（主记录 + 处理日志）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WarningRecordDetailVO extends WarningRecordVO {

    /**
     * 处理日志（按时间倒序）
     */
    private List<WarningProcessLogVO> processLogs;
}
