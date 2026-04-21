package com.yutu.modules.user.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class MerchantApplicationSaveRequest {
    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 100, message = "店铺名称长度不能超过100个字符")
    private String shopName;

    @NotBlank(message = "联系人不能为空")
    @Size(max = 64, message = "联系人长度不能超过64个字符")
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^\\d{11}$", message = "手机号必须为11位数字")
    private String contactPhone;

    @Size(max = 255, message = "店铺简介长度不能超过255个字符")
    private String description;

    @NotBlank(message = "营业执照号不能为空")
    @Size(max = 64, message = "营业执照号长度不能超过64个字符")
    private String licenseNo;

    @NotBlank(message = "请上传营业执照")
    private String licenseImage;

    @NotBlank(message = "请上传身份证人像面")
    private String idCardFrontImage;

    @NotBlank(message = "请上传身份证国徽面")
    private String idCardBackImage;
}
