package com.mortal.regulation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mortal.regulation.common.enums.ComplaintStatus;
import com.mortal.regulation.common.enums.TaskSourceType;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("complaint")
public class Complaint {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String complaintNo;
    private String complainantName;
    private String contact;
    private Long enterpriseId;
    private String complaintType;
    private String content;
    private ComplaintStatus status;
    private TaskSourceType sourceType;
    private Long sourceId;
    private Long assignedTo;
    private Long assignedBy;
    private LocalDateTime assignedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
