package com.mortal.complaint.dto;

import lombok.Data;

/**
 * 内部统计范围查询参数。
 */
@Data
public class InternalStatsQueryDTO {

    private Long regionId;

    private String regionIds;

    private Long ownerRegulatorId;
}
