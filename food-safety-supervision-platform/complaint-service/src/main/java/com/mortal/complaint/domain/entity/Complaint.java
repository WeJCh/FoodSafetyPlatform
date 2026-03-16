package com.mortal.complaint.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mortal.complaint.domain.enums.ComplaintStatus;
import com.mortal.complaint.domain.enums.TaskSourceType;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 投诉实体
 */
@Data
@TableName("complaint")
public class Complaint {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String complaintNo;
    private String complainantName;
    private String contact;
    private Long submitterUserId;
    private Long enterpriseId;
    private String complaintType;
    private String content;
    @TableField("image_urls")
    private String imageUrls;
    private ComplaintStatus status;
    private TaskSourceType sourceType;
    private Long sourceId;
    private Long assignedTo;
    private Long assignedBy;
    private LocalDateTime assignedTime;
    private Long acceptedBy;
    private LocalDateTime acceptedTime;
    private Long processedBy;
    private LocalDateTime processedTime;
    private Long rejectedBy;
    private LocalDateTime rejectedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
