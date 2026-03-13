package com.mortal.query.vo;

import java.util.List;
import lombok.Data;

@Data
public class RegulatorProfileVO {
    private Long id;
    private Long userId;
    private String roleType;
    private List<Long> regionIds;
}

