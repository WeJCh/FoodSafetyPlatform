package com.mortal.regulation.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProductVO {

    private Long id;
    private Long enterpriseId;
    private String productName;
    private String category;
    private String specification;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
