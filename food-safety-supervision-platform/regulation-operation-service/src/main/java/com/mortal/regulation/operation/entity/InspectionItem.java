package com.mortal.regulation.operation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("inspection_item")
public class InspectionItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inspectionId;
    private String itemName;
    private String itemResult;
    private String problemDesc;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
