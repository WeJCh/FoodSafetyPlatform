package com.mortal.regulation.operation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 抽检结果实体类
 */
@Data
@TableName("sampling_result")
public class SamplingResult {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long enterpriseId;
    private Long productId;
    private Long sampledBy;
    private LocalDateTime sampledTime;
    private String result;
    private String conclusion;
    private String disposalSuggestion;
    private String publicStatus;
    private LocalDateTime publishedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
