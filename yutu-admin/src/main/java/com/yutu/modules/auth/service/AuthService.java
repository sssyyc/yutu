package com.yutu.modules.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yutu.common.context.UserContext;
import com.yutu.common.exception.BizException;
import com.yutu.common.util.JwtUtil;
import com.yutu.modules.auth.dto.LoginRequest;
import com.yutu.modules.auth.dto.RegisterRequest;
import com.yutu.modules.auth.vo.AuthLoginVO;
import com.yutu.modules.auth.vo.AuthMeVO;
import com.yutu.modules.model.entity.SysMenu;
import com.yutu.modules.model.entity.SysUser;
import com.yutu.modules.model.mapper.SysMenuMapper;
import com.yutu.modules.model.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {
    private final SysUserMapper sysUserMapper;
    private final SysMenuMapper sysMenuMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.token.redis-prefix}")
    private String tokenPrefix;

    public AuthService(SysUserMapper sysUserMapper,
                       SysMenuMapper sysMenuMapper,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       StringRedisTemplate stringRedisTemplate) {
        this.sysUserMapper = sysUserMapper;
        this.sysMenuMapper = sysMenuMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public AuthLoginVO login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
                .eq(SysUser::getStatus, 1)
                .last("limit 1"));
        if (user == null) {
            throw new BizException(400, "用户名或密码错误");
        }
        boolean matched;
        if (StringUtils.hasText(user.getPassword()) && user.getPassword().startsWith("$2a$")) {
            matched = passwordEncoder.matches(request.getPassword(), user.getPassword());
        } else {
            matched = request.getPassword().equals(user.getPassword());
            if (matched) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                sysUserMapper.updateById(user);
            }
        }
        if (!matched) {
            throw new BizException(400, "用户名或密码错误");
        }

        List<String> roleCodes = roleCodesByType(user.getRoleType());
        List<String> perms = loadPerms(roleCodes);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("roleType", user.getRoleType());
        claims.put("permsVersion", System.currentTimeMillis());
        claims.put("perms", perms);
        String token = jwtUtil.createToken(claims);
        stringRedisTemplate.opsForValue().set(tokenPrefix + token, String.valueOf(user.getId()), jwtUtil.getExpireSeconds(), TimeUnit.SECONDS);

        AuthLoginVO vo = new AuthLoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRoleType(user.getRoleType());
        vo.setPerms(perms);
        return vo;
    }

    public Long register(RegisterRequest request) {
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (count != null && count > 0) {
            throw new BizException(400, "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setRoleType(1);
        user.setStatus(1);
        user.setDeleted(0);
        sysUserMapper.insert(user);
        return user.getId();
    }

    public AuthMeVO me() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(401, "未登录");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }

        List<String> roleCodes = roleCodesByType(user.getRoleType());
        List<String> perms = loadPerms(roleCodes);
        List<SysMenu> menus = loadMenus(roleCodes);

        AuthMeVO vo = new AuthMeVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setRoleType(user.getRoleType());
        vo.setPerms(perms);
        vo.setMenus(menus);
        return vo;
    }

    private List<String> roleCodesByType(Integer roleType) {
        if (roleType == null) {
            throw new BizException(400, "角色无效");
        }
        if (roleType == 1) {
            return Collections.singletonList("USER");
        }
        if (roleType == 2) {
            return Arrays.asList("USER", "MERCHANT");
        }
        if (roleType == 3) {
            return Collections.singletonList("ADMIN");
        }
        throw new BizException(400, "角色无效");
    }

    private List<String> loadPerms(List<String> roleCodes) {
        LinkedHashSet<String> perms = new LinkedHashSet<>();
        for (String roleCode : roleCodes) {
            List<String> rolePerms = sysMenuMapper.selectPermsByRoleCode(roleCode);
            if (rolePerms != null) {
                perms.addAll(rolePerms);
            }
        }
        return new ArrayList<>(perms);
    }

    private List<SysMenu> loadMenus(List<String> roleCodes) {
        LinkedHashMap<Long, SysMenu> menuMap = new LinkedHashMap<>();
        for (String roleCode : roleCodes) {
            List<SysMenu> roleMenus = sysMenuMapper.selectByRoleCode(roleCode);
            if (roleMenus == null) {
                continue;
            }
            for (SysMenu menu : roleMenus) {
                if (menu == null || menu.getId() == null) {
                    continue;
                }
                menuMap.putIfAbsent(menu.getId(), menu);
            }
        }
        return new ArrayList<>(menuMap.values());
    }
}
