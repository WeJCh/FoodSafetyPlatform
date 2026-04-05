package com.mortal.regulation.vo;

import java.time.LocalDateTime;
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
    private String addressDetail;
    private String status;
    private LocalDateTime approvedTime;
    private LocalDateTime updateTime;
}
