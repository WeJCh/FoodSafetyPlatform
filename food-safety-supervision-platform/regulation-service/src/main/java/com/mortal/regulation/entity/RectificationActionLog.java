package com.mortal.regulation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("rectification_action_log")
public class RectificationActionLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long rectificationId;
    private String actionType;
    private Long operatorId;
    private String actionComment;
    private String attachmentUrls;
    private LocalDateTime createTime;
    private Integer deleted;
}
