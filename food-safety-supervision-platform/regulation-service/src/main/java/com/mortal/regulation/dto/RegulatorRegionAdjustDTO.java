package com.mortal.regulation.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class RegulatorRegionAdjustDTO {

    @NotEmpty
    private List<Long> regionIds;
    private String remark;
}
