package com.mortal.regulation.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 整改任务操作日志VO
 */
@Data
public class RectificationActionLogVO {

    private Long id;
    private Long rectificationId;
    private String actionType;
    private String actionName;
    private Long operatorId;
    private String operatorName;
    private String actionComment;
    private List<String> attachmentUrls;
    private LocalDateTime createTime;
}
