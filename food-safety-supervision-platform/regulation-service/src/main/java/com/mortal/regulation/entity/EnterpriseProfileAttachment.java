package com.mortal.regulation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("enterprise_profile_attachment")
public class EnterpriseProfileAttachment {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private String attachmentType;
    private String attachmentName;
    private String attachmentUrl;
    private Long uploadedBy;
    private LocalDateTime uploadedAt;
    private Integer deleted;
}
