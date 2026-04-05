package com.mortal.regulation.operation.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SamplingTaskVO {

    private Long id;
    private String taskNo;
    private Long enterpriseId;
    private String enterpriseName;
    private Long productId;
    private String productName;
    private String productCategory;
    private String productSpecification;
    private Long regionId;
    private String taskTitle;
    private String taskDesc;
    private String priority;
    private String status;
    private Long createdBy;
    private String createdByName;
    private Long assignedTo;
    private String assignedToName;
    private Long assignedBy;
    private String assignedByName;
    private LocalDateTime assignedTime;
    private LocalDateTime completedTime;
    private Long samplingResultId;
    private String samplingResult;
    private String samplingConclusion;
    private String samplingPublicStatus;
    private LocalDateTime samplingPublishedTime;
    private LocalDateTime sampledTime;
    private LocalDateTime deadline;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
