package com.mortal.warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
/**
 * 预警记录实体类
 */
@Data
@TableName("warning_record")
public class WarningRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 预警编号
     */
    private String warningNo;
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
     * 辖区ID（用于管理员权限过滤）
     */
    private Long regionId;
    /**
     * 责任执法员ID（用于执法员权限过滤）
     */
    private Long ownerRegulatorId;
    /**
     * 去重key
     */
    private String dedupKey;
    /**
     * 预警级别
     */
    private String level;
    /**
     * 预警状态
     */
    private String status;
    /**
     * 预警标题
     */
    private String title;
    /**
     * 预警内容
     */
    private String content;
    /**
     * 预警来源
     */
    private String sourceService;
    /**
     * 首次发生时间
     */
    private LocalDateTime firstOccurTime;
    /**
     * 最后发生时间
     */
    private LocalDateTime lastOccurTime;
    /**
     * 触发次数
     */
    private Integer triggerCount;
    /**
     * 指派处理人ID
     */
    private Long assignedTo;
    /**
     * 指派时间
     */
    private LocalDateTime assignedTime;
    /**
     * 解决人ID
     */
    private Long resolvedBy;
    /**
     * 解决时间
     */
    private LocalDateTime resolvedTime;
    /**
     * 关闭原因
     */
    private String closeReason;
    /**
     * 预警数据
     */
    private String payloadJson;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 是否删除
     */
    private Integer deleted;
}
