package com.mortal.complaint.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 投诉处理VO
 */
@Data
public class ComplaintHandleVO {

    private Long handlerId;
    private String handlerName;
    private String handleResult;
    private LocalDateTime handleTime;
}
