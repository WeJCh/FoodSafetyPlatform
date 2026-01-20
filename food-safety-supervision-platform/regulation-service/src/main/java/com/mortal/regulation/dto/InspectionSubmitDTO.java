package com.mortal.regulation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class InspectionSubmitDTO {

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate inspectionDate;

    @NotBlank
    @Size(max = 20)
    private String result;

    @Size(max = 1000)
    private String problemDesc;

    @Valid
    private List<InspectionItemDTO> items;
}
