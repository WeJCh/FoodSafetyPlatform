package com.mortal.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class InspectionTaskCreateDTO {

    @NotNull
    private Long enterpriseId;

    @NotBlank
    @Size(max = 100)
    private String taskTitle;

    @Size(max = 500)
    private String taskDesc;

    @Size(max = 10)
    private String priority;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deadline;
}
