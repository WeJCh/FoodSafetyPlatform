package com.mortal.regulation.operation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 抽检任务实体类
 */
@Data
@TableName("sampling_task")
public class SamplingTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskNo;
    private Long enterpriseId;
    private Long productId;
    private Long regionId;
    private String taskTitle;
    private String taskDesc;
    private String priority;
    private String status;
    private Long createdBy;
    private Long assignedTo;
    private Long assignedBy;
    private LocalDateTime assignedTime;
    private LocalDateTime completedTime;
    private LocalDateTime deadline;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
