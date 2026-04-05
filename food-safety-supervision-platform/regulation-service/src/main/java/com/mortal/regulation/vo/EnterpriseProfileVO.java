package com.mortal.regulation.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class EnterpriseProfileVO {

    private Long id;
    private Long userId;
    private String enterpriseName;
    private String licenseNo;
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
    private List<EnterpriseKeyReasonVO> keyReasons;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
