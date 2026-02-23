package com.mortal.regulation.vo;

import com.mortal.regulation.common.enums.ComplaintStatus;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ComplaintListVO {

    private Long id;
    private String complaintNo;
    private Long enterpriseId;
    private String enterpriseName;
    private ComplaintStatus status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
