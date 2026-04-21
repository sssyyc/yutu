package com.yutu.modules.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tour_category")
public class TourCategory extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String categoryName;
    private Integer sortNum;
    private Integer status;
    @TableLogic
    private Integer deleted;
}
