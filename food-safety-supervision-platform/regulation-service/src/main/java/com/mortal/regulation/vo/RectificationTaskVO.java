package com.mortal.regulation.vo;

import com.mortal.regulation.common.enums.RectificationStatus;
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
    private LocalDateTime finishTime;
    private Long confirmedBy;
    private String confirmedByName;
    private LocalDateTime confirmedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

