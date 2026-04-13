package com.mortal.complaint.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 浼佷笟姒傚喌VO
 */
@Data
public class EnterpriseProfileVO {

    private Long id;
    private Long userId;
    private String enterpriseName;
    private String licenseNo;
    private String creditCode;
    private String legalRepresentative;
    private Long regionId;
    private Long addressId;
    private String addressDetail;
    private String principal;
    private String principalPhone;
    private String regulatorName;
    private String status;
    private String approvalStatus;
    private String approvalComment;
    private Long approvedBy;
    private LocalDateTime approvedTime;
    private List<RegionVO> regionPath;
    private String regionPathText;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

