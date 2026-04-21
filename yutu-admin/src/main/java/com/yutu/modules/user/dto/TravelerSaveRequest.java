package com.yutu.modules.user.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class TravelerSaveRequest {
    @NotBlank(message = "姓名不能为空")
    private String travelerName;

    @NotBlank(message = "身份证不能为空")
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号必须18位（最后一位可为X）")
    private String idCard;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^\\d{11}$", message = "手机号必须为11位数字")
    private String phone;
}
