package com.yutu.modules.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contract_template")
public class ContractTemplate extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String templateName;
    private String templateCode;
    private String versionNo;
    private String templateContent;
    private Long downloadCount;
    private Long useCount;
    private Integer status;
    private String remark;
    @TableLogic
    private Integer deleted;
}
