package com.mortal.regulation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mortal.regulation.common.enums.EnterpriseKeyReasonType;
import com.mortal.regulation.common.enums.TaskSourceType;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("enterprise_key_reason")
public class EnterpriseKeyReason {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private EnterpriseKeyReasonType reasonType;
    private String reasonDetail;
    private TaskSourceType sourceType;
    private Long sourceId;
    private Long operatorId;
    private LocalDateTime createTime;
}
