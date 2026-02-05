package com.mortal.regulation.vo;

import com.mortal.regulation.common.enums.ComplaintStatus;
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
