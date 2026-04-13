package com.mortal.regulation.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class EnterpriseProfileAttachmentVO {

    private Long id;
    private String type;
    private String label;
    private String name;
    private String url;
    private Long uploadedBy;
    private LocalDateTime uploadedAt;
}
