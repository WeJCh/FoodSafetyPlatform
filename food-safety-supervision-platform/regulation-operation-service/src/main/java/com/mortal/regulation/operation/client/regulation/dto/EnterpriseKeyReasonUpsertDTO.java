package com.mortal.regulation.operation.client.regulation.dto;

import lombok.Data;

/**
 * 企业关键原因插入DTO
 */
@Data
public class EnterpriseKeyReasonUpsertDTO {

    private String reasonType;
    private String reasonDetail;
    private String sourceType;
    private Long sourceId;
    private Long operatorId;
}
