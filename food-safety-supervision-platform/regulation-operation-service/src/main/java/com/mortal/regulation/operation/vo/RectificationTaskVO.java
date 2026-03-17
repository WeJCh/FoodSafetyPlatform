package com.mortal.regulation.operation.vo;

import com.mortal.regulation.operation.common.enums.RectificationStatus;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RectificationTaskVO {

    private Long id;
    private Long inspectionId;
    private Long enterpriseId;
    private String enterpriseName;
    private String rectificationDesc;
    private String progress;
    private RectificationStatus status;
    private LocalDateTime submitDeadline;
    private LocalDateTime reviewDeadline;
    private LocalDateTime currentDeadline;
    private String slaStage;
    private String slaStatus;
    private Long remainingMinutes;
    private LocalDateTime finishTime;
    private Long confirmedBy;
    private String confirmedByName;
    private LocalDateTime confirmedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
