package com.mortal.complaint.vo;

import com.mortal.complaint.domain.enums.ComplaintStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 投诉VO
 */
@Data
public class ComplaintVO {

    private Long id;
    private String complaintNo;
    private Long enterpriseId;
    private String enterpriseName;
    private String complaintType;
    private String content;
    private List<String> imageUrls;
    private Long acceptedBy;
    private String acceptedByName;
    private LocalDateTime acceptedTime;
    private ComplaintStatus status;
    private Long assignedTo;
    private String assignedToName;
    private Long assignedBy;
    private String assignedByName;
    private LocalDateTime assignedTime;
    private Long processedBy;
    private String processedByName;
    private LocalDateTime processedTime;
    private Long rejectedBy;
    private String rejectedByName;
    private LocalDateTime rejectedTime;
    private String handleResult;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
