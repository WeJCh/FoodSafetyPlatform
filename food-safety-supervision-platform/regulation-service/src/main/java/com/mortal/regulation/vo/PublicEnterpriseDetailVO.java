package com.mortal.regulation.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 鍏紬浼佷笟璇︽儏VO
 */
@Data
public class PublicEnterpriseDetailVO {

    private Long id;
    private String enterpriseName;
    private String licenseNo;
    private String creditCode;
    private String legalRepresentative;
    private Long regionId;
    private String regionPathText;
    private String addressDetail;
    private String principal;
    private String principalPhoneMasked;
    private String regulatorName;
    private String status;
    private List<EnterpriseProfileAttachmentVO> attachments;
    private List<EnterpriseKeyReasonVO> keyReasons;
    private LocalDateTime approvedTime;
    private LocalDateTime updateTime;
}

