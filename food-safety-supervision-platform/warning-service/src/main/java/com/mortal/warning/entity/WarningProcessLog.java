package com.mortal.warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 预警处理日志实体类
 */
@Data
@TableName("warning_process_log")
public class WarningProcessLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long warningId;
    private String actionType;
    private Long operatorId;
    private String operatorName;
    private String actionComment;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
