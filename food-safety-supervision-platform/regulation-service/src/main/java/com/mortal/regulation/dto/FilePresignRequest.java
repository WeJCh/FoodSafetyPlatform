package com.mortal.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文件预签名请求DTO
 */
@Data
public class FilePresignRequest {

    @NotBlank
    private String filename;

    @NotBlank
    private String contentType;

    @NotNull
    private Long size;
}
