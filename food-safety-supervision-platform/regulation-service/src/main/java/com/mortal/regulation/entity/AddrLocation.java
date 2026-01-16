package com.mortal.regulation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("addr_location")
public class AddrLocation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long regionId;
    private String detail;
    private Integer deleted;
}
