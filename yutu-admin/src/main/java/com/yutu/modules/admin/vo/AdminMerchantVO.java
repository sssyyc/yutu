package com.yutu.modules.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminMerchantVO {
    private Long id;
    private Long userId;
    private String applicantUsername;
    private String applicantNickname;
    private String applicantPhone;
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
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime auditTime;
}
