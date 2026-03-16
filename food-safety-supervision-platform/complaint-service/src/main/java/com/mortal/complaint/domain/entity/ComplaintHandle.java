package com.mortal.complaint.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 投诉处理实体
 */
@Data
@TableName("complaint_handle")
public class ComplaintHandle {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long complaintId;
    private Long handlerId;
    private String handleResult;
    private LocalDateTime handleTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
