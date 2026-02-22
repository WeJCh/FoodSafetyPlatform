package com.mortal.regulation.vo;

import com.mortal.regulation.common.enums.ComplaintStatus;
import java.time.LocalDateTime;
import java.util.List;
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
     * 现场图片地址列表
     */
    private List<String> imageUrls;
    /**
     * 受理人ID
     */
    private Long acceptedBy;
    /**
     * 受理人姓名
     */
    private String acceptedByName;
    /**
     * 受理时间
     */
    private LocalDateTime acceptedTime;
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
    /**
     * 处理完成人ID
     */
    private Long processedBy;
    /**
     * 处理完成人姓名
     */
    private String processedByName;
    /**
     * 处理完成时间
     */
    private LocalDateTime processedTime;
    /**
     * 驳回人ID
     */
    private Long rejectedBy;
    /**
     * 驳回人姓名
     */
    private String rejectedByName;
    /**
     * 驳回时间
     */
    private LocalDateTime rejectedTime;
    private String handleResult;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
