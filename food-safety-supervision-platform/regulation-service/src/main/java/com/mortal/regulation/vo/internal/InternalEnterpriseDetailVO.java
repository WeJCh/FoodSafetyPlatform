package com.mortal.regulation.vo.internal;

import com.mortal.regulation.vo.EnterpriseProfileAttachmentVO;
import java.util.List;
import lombok.Data;

@Data
public class InternalEnterpriseDetailVO {

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
    private List<EnterpriseProfileAttachmentVO> attachments;
}

