package com.mortal.regulation.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class RegulatorProfileVO {

    private Long id;
    private Long userId;
    private String name;
    private String phone;
    private String roleType;
    private List<Long> regionIds;
    private Integer status;
    private String workIdUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
