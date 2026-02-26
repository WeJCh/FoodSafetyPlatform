package com.mortal.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class RectificationSubmitDTO {

    @NotBlank(message = "progress required")
    @Size(max = 1000)
    private String progress;
}

