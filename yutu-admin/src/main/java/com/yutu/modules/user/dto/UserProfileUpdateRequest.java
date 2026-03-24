package com.yutu.modules.user.dto;

import lombok.Data;

import javax.validation.constraints.Pattern;

@Data
public class UserProfileUpdateRequest {
    private String nickname;
    private String avatar;

    @Pattern(regexp = "^$|^\\d{11}$", message = "手机号必须为11位数字")
    private String phone;
}
