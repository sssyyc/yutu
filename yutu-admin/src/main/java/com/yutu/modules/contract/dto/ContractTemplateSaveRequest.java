package com.yutu.modules.contract.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ContractTemplateSaveRequest {
    @NotBlank(message = "template name cannot be blank")
    private String templateName;

    @NotBlank(message = "template code cannot be blank")
    private String templateCode;

    @NotBlank(message = "template type cannot be blank")
    private String templateType;

    @NotBlank(message = "apply scope cannot be blank")
    private String applyScope;

    @NotBlank(message = "version cannot be blank")
    private String versionNo;

    @NotBlank(message = "template content cannot be blank")
    private String templateContent;

    private String remark;
}
