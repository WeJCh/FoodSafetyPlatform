package com.mortal.regulation.operation.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class InspectionRecordVO {

    private Long id;
    private Long enterpriseId;
    private String enterpriseName;
    private LocalDate inspectionDate;
    private String result;
    private String problemDesc;
    private LocalDateTime updateTime;
}
