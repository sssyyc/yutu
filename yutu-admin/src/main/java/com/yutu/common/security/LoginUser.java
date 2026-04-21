package com.yutu.common.security;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class LoginUser implements Serializable {
    private Long userId;
    private String username;
    private Integer roleType;
    private List<String> perms = new ArrayList<>();
}
