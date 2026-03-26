package com.yutu.modules.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tour_contract_signature")
public class TourContractSignature extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private Long userId;
    private Long travelerId;
    private String signerName;
    private String signatureImage;
    private LocalDateTime signTime;
}
