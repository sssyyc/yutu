package com.yutu.modules.refund.dto;

import lombok.Data;

import java.util.List;

@Data
public class RefundSupplementRequest {
    private String content;
    private List<String> evidenceUrls;
}
