package com.yutu.modules.auth.vo;

import lombok.Data;

import java.util.List;

@Data
public class AuthLoginVO {
    private String token;
    private Long userId;
    private String username;
    private String nickname;
    private Integer roleType;
    private List<String> perms;
}
