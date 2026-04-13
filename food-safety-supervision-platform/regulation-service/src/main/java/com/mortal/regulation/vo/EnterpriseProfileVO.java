package com.mortal.regulation.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class EnterpriseProfileVO {

    private Long id;
    private Long userId;
    private String enterpriseName;
    private String licenseNo;
    @JsonProperty("creditCode")
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
    private List<EnterpriseProfileAttachmentVO> attachments;
    private List<EnterpriseKeyReasonVO> keyReasons;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
