package com.mortal.regulation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mortal.regulation.common.enums.RectificationStatus;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("rectification_task")
public class RectificationTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inspectionId;
    private Long enterpriseId;
    private String rectificationDesc;
    private String progress;
    private RectificationStatus status;
    private LocalDateTime finishTime;
    private Long confirmedBy;
    private LocalDateTime confirmedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
