package com.yutu.modules.order.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class OrderReviewRequest {
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小为1分")
    @Max(value = 5, message = "评分最大为5分")
    private Integer score;

    @Size(max = 255, message = "评价内容不能超过255个字符")
    private String content;
}
