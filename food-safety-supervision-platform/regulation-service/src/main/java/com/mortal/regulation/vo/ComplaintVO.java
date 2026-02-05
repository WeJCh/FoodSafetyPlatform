package com.mortal.regulation.vo;

import com.mortal.regulation.common.enums.ComplaintStatus;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 投诉VO
 */
@Data
public class ComplaintVO {

    /**
     * 投诉ID
     */
    private Long id;
    /**
     * 投诉编号
     */
    private String complaintNo;
    /**
     * 企业ID
     */
    private Long enterpriseId;
    /**
     * 企业名称
     */
    private String enterpriseName;
    /**
     * 投诉类型
     */
    private String complaintType;
    /**
     * 投诉内容
     */
    private String content;
    /**
     * 投诉状态
     */
    private ComplaintStatus status;
    private Long assignedTo;
    /**
     * 被指派去处理投诉的执行人姓名
     */
    private String assignedToName;
    private Long assignedBy;
    /**
     * 做出指派操作的指派人姓名
     */
    private String assignedByName;
    private LocalDateTime assignedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
