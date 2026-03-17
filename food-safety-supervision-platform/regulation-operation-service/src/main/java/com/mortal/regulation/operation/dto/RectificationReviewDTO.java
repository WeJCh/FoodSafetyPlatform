package com.mortal.regulation.operation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class RectificationReviewDTO {

    @NotBlank(message = "action required")
    private String action;

    @Size(max = 1000, message = "comment too long")
    private String comment;

    @Size(max = 10, message = "too many attachments")
    private List<String> attachmentUrls;
}
