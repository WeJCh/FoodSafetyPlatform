package com.mortal.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/**
 * 投诉提交DTO
 */
@Data
public class ComplaintSubmitDTO {

    @Size(max = 50)
    private String complainantName;

    @Size(max = 50)
    private String contact;

    @NotNull
    private Long enterpriseId;

    @Size(max = 50)
    private String complaintType;

    @NotBlank
    @Size(max = 2000)
    private String content;

    @Size(max = 5)
    private List<String> imageUrls;
}
