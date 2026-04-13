package com.mortal.regulation.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 公共企业信息VO
 */
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class PublicEnterpriseVO {

    private Long id;
    private String enterpriseName;
    @JsonProperty("creditCode")
    private String creditCode;
    private Long regionId;
    private String regionPathText;
    private String addressDetail;
    private String status;
    private LocalDateTime approvedTime;
    private LocalDateTime updateTime;
}
