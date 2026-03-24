package com.yutu.modules.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_shop")
public class MerchantShop extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
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
    private LocalDateTime auditTime;
    private Integer status;
    @TableLogic
    private Integer deleted;
}
