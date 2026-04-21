package com.yutu.modules.auth.vo;

import com.yutu.modules.model.entity.SysMenu;
import lombok.Data;

import java.util.List;

@Data
public class AuthMeVO {
    private Long userId;
    private String username;
    private String nickname;
    private String phone;
    private String avatar;
    private Integer roleType;
    private List<String> perms;
    private List<SysMenu> menus;
}
