package com.mortal.regulation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("food_regulator")
public class FoodRegulator {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String phone;
    private String roleType;
    private String jurisdictionArea;
    private Integer status;
    private String workIdUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
