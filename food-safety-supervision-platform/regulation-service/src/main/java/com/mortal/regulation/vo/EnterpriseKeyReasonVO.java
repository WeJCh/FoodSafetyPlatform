package com.mortal.regulation.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 企业关键原因VO
 */
@Data
public class EnterpriseKeyReasonVO {

    private String reasonType;
    private String reasonLabel;
    private String reasonDetail;
    private String sourceType;
    private Long sourceId;
    private LocalDateTime createTime;
}
