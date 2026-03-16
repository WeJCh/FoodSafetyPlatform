package com.mortal.complaint.vo;

import lombok.Data;

/**
 * 区域VO
 */
@Data
public class RegionVO {

    private Long id;
    private Long parentId;
    private String name;
    private Integer level;
}
