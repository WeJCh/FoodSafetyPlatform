package com.mortal.regulation.operation.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class RectificationActionLogVO {

    private Long id;
    private Long rectificationId;
    private String rectificationNo;
    private Long enterpriseId;
    private String enterpriseName;
    private String status;
    private String actionType;
    private String actionName;
    private Long operatorId;
    private String operatorName;
    private String actionComment;
    private List<String> attachmentUrls;
    private LocalDateTime createTime;
}
