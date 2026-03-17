package com.mortal.regulation.operation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class RectificationSubmitDTO {

    @NotBlank(message = "progress required")
    @Size(max = 1000)
    private String progress;

    @Size(max = 10, message = "too many attachments")
    private List<String> attachmentUrls;
}
