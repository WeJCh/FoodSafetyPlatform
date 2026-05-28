package com.mortal.regulation.dto;

import lombok.Data;

/**
 * 预警记录查询参数。
 */
@Data
public class WarningRecordQueryDTO {

    /**
     * 页码（从1开始）
     */
    private Integer page = 1;

    /**
     * 每页条数
     */
    private Integer size = 10;

    /**
     * 状态
     */
    private String status;

    /**
     * 等级
     */
    private String level;

    /**
     * 预警类型
     */
    private String warningType;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 业务ID
     */
    private Long bizId;

    /**
     * 关键字
     */
    private String keyword;

    /**
     * 分派处理人ID（执法员「我的预警」列表过滤）
     */
    private Long assignedTo;

    /**
     * 辖区ID列表（逗号分隔，作用域过滤）
     */
    private String regionIds;
}
