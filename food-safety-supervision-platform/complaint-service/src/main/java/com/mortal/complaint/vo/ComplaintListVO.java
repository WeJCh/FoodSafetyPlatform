package com.mortal.complaint.vo;

import com.mortal.complaint.domain.enums.ComplaintStatus;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 投诉列表VO
 */
@Data
public class ComplaintListVO {

    private Long id;
    private String complaintNo;
    private Long enterpriseId;
    private String enterpriseName;
    private String complaintType;
    private String content;
    private String handleResult;
    private ComplaintStatus status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
