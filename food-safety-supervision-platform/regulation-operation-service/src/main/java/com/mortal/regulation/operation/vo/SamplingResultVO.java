package com.mortal.regulation.operation.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SamplingResultVO {

    private Long id;
    private Long taskId;
    private String taskNo;
    private Long enterpriseId;
    private String enterpriseName;
    private Long productId;
    private String productName;
    private String productCategory;
    private String productSpecification;
    private Long sampledBy;
    private String sampledByName;
    private LocalDateTime sampledTime;
    private String result;
    private String conclusion;
    private String disposalSuggestion;
    private String publicStatus;
    private LocalDateTime publishedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
