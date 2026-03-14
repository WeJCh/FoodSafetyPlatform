package com.mortal.query.vo;

import lombok.Data;

@Data
public class RegionVO {
    private Long id;
    private Long parentId;
    private String name;
    private Integer level;
}
