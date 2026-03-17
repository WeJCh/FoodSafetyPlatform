package com.mortal.regulation.operation.client.regulation.vo;

import lombok.Data;

@Data
public class InternalEnterpriseDetailVO {

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
}
