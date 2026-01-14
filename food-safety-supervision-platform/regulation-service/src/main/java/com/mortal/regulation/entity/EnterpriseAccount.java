package com.mortal.regulation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("enterprise_account")
public class EnterpriseAccount {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long enterpriseId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
