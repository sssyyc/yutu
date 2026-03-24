package com.yutu.modules.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MerchantApplicationVO {
    private Long id;
    private Long userId;
    private Integer roleType;
    private String shopName;
    private String contactName;
    private String contactPhone;
    private String description;
    private String licenseNo;
    private String licenseImage;
    private String idCardFrontImage;
    private String idCardBackImage;
    private Integer auditStatus;
    private String auditRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime auditTime;
}
