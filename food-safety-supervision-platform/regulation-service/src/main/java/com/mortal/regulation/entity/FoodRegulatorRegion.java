package com.mortal.regulation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("food_regulator_region")
public class FoodRegulatorRegion {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long regulatorId;
    private Long regionId;
    private Integer deleted;
}
