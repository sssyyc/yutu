package com.yutu.modules.contract.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ContractTemplateSaveRequest {
    @NotBlank(message = "模板名称不能为空")
    private String templateName;
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;
    @NotBlank(message = "版本号不能为空")
    private String versionNo;
    @NotBlank(message = "模板内容不能为空")
    private String templateContent;
    private String remark;
}
