package com.mortal.regulation.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class BulletinVO {

    private Long id;
    private String title;
    private String category;
    private String status;
    private Long createdBy;
    private String createdByName;
    private Long publishedBy;
    private String publishedByName;
    private LocalDateTime publishedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
