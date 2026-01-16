package com.mortal.regulation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("food_enterprise")
public class FoodEnterprise {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String enterpriseName;
    private String licenseNo;
    private Long regionId;
    private Long addressId;
    private String principal;
    private String principalPhone;
    private String regulatorName;
    private String status;
    private String approvalStatus;
    private String approvalComment;
    private Long approvedBy;
    private LocalDateTime approvedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
