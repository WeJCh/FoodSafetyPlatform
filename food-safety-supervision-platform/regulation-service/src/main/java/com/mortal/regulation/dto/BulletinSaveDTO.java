package com.mortal.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BulletinSaveDTO {

    @NotBlank
    @Size(max = 120)
    private String title;

    @Size(max = 255)
    private String summary;

    @NotBlank
    @Size(max = 4000)
    private String content;
}
