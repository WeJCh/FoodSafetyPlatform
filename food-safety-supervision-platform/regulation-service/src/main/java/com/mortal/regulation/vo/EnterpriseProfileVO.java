package com.mortal.regulation.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class EnterpriseProfileVO {

    private Long id;
    private Long userId;
    private String enterpriseName;
    private String licenseNo;
    private String address;
    private String principal;
    private String principalPhone;
    private String regulatorName;
    private String status;
    private String approvalStatus;
    private String approvalComment;
    private Long approvedBy;
    private LocalDateTime approvedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
