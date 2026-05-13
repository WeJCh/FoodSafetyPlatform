package com.mortal.regulation.vo;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 预警详情视图。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WarningRecordDetailVO extends WarningRecordVO {

    /**
     * 处理日志。
     */
    private List<WarningProcessLogVO> processLogs;
}
