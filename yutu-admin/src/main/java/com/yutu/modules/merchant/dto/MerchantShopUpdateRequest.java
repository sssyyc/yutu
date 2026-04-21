package com.yutu.modules.merchant.dto;

import lombok.Data;

import javax.validation.constraints.Pattern;

@Data
public class MerchantShopUpdateRequest {
    private String shopName;
    private String contactName;

    @Pattern(regexp = "^$|^\\d{11}$", message = "手机号必须为11位数字")
    private String contactPhone;

    private String description;
    private String licenseNo;
}
