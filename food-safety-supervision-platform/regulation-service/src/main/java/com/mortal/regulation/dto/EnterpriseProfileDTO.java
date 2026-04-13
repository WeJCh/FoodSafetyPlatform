package com.mortal.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class EnterpriseProfileDTO {

    @NotBlank
    private String enterpriseName;
    private String licenseNo;
    /**
     * 统一社会信用代码（18 位）；空表示暂未填写。
     */
    @Size(max = 18)
    @Pattern(
        regexp = "^$|^[0-9A-HJ-NPQRTUWXY]{2}\\d{6}[0-9A-HJ-NPQRTUWXY]{10}$",
        message = "creditCode format invalid",
        flags = Pattern.Flag.CASE_INSENSITIVE
    )
    private String creditCode;
    private String legalRepresentative;
    @NotNull
    private Long regionId;
    @NotBlank
    private String addressDetail;
    private String principal;
    @Pattern(regexp = "^\\d{11}$", message = "principalPhone must be 11 digits")
    private String principalPhone;
    private List<EnterpriseProfileAttachmentDTO> attachments;
}
