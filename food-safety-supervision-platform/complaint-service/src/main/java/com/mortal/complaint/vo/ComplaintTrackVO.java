package com.mortal.complaint.vo;

import com.mortal.complaint.domain.enums.ComplaintStatus;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 投诉跟踪VO
 */
@Data
public class ComplaintTrackVO {

    private String complaintNo;
    private ComplaintStatus status;
    private LocalDateTime updateTime;
}
