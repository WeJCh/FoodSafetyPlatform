package com.mortal.regulation.operation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("inspection_record")
public class InspectionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long enterpriseId;
    private Long inspectorId;
    private LocalDate inspectionDate;
    private String result;
    private String problemDesc;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
