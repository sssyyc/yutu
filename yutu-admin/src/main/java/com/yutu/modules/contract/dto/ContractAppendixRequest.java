package com.yutu.modules.contract.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ContractAppendixRequest {
    @NotBlank(message = "附件标题不能为空")
    private String appendixTitle;
    @NotBlank(message = "附件内容不能为空")
    private String appendixContent;
}
