package com.mortal.regulation.operation.vo;

import com.mortal.regulation.operation.common.enums.RectificationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class InspectionRecordVO {

    private Long id;
    private Long taskId;
    private String taskNo;
    private String taskTitle;
    private Long enterpriseId;
    private String enterpriseName;
    private String creditCode;
    private String enterpriseAddress;
    private LocalDate inspectionDate;
    private String result;
    private String problemDesc;
    private Long rectificationId;
    private RectificationStatus rectificationStatus;
    private LocalDateTime updateTime;
}
