package com.mortal.regulation.vo;

import lombok.Data;

/**
 * 公共企业信息VO
 */
@Data
public class PublicEnterpriseVO {

    private Long id;
    private String enterpriseName;
    private Long regionId;
    private String regionPathText;
}
