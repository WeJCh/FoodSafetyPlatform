package com.mortal.regulation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("food_product")
public class FoodProduct {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private String productName;
    private String category;
    private String specification;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
