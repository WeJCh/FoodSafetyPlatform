package com.mortal.regulation.operation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 抽检结果提交DTO
 */
@Data
public class SamplingResultSubmitDTO {

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sampledTime;

    @NotBlank
    @Size(max = 20)
    private String result;

    @Size(max = 500)
    private String conclusion;

    @Size(max = 500)
    private String disposalSuggestion;
}
