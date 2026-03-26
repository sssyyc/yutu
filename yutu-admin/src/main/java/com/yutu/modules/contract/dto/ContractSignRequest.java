package com.yutu.modules.contract.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class ContractSignRequest {
    @NotNull(message = "Traveler is required")
    private Long travelerId;

    @NotBlank(message = "Signer name is required")
    @Size(max = 64, message = "Signer name is too long")
    private String signerName;

    @NotBlank(message = "Signature image is required")
    private String signatureImage;
}
