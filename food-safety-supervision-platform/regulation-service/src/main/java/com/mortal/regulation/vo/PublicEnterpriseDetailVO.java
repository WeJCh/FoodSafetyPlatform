package com.mortal.regulation.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 公众企业详情VO
 */
@Data
public class PublicEnterpriseDetailVO {

    private Long id;
    private String enterpriseName;
    private String licenseNo;
    private Long regionId;
    private String regionPathText;
    private String addressDetail;
    private String principal;
    private String principalPhoneMasked;
    private String regulatorName;
    private String status;
    private List<EnterpriseKeyReasonVO> keyReasons;
    private LocalDateTime approvedTime;
    private LocalDateTime updateTime;
}
