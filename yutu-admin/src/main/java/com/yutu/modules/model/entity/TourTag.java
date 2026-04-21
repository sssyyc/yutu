package com.yutu.modules.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tour_tag")
public class TourTag extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tagName;
    private String tagType;
    private Integer status;
    @TableLogic
    private Integer deleted;
}
