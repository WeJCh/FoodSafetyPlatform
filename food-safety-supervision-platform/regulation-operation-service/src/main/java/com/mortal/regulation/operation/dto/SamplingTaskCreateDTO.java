package com.mortal.regulation.operation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;
/**
 * 抽检任务创建DTO
 */
@Data
public class SamplingTaskCreateDTO {

    @NotNull
    private Long enterpriseId;

    @NotNull
    private Long productId;

    @NotBlank
    @Size(max = 100)
    private String taskTitle;

    @Size(max = 500)
    private String taskDesc;

    @Size(max = 10)
    private String priority;

    @NotNull(message = "deadline required")
    @Future(message = "deadline must be future")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deadline;
}
