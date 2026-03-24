package com.yutu.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yutu.common.result.Result;
import com.yutu.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.token.redis-prefix}")
    private String tokenPrefix;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, StringRedisTemplate stringRedisTemplate) {
        this.jwtUtil = jwtUtil;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            String redisKey = tokenPrefix + token;
            if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(redisKey))) {
                writeUnauthorized(response, "登录已过期");
                return;
            }

            Claims claims = jwtUtil.parseToken(token);
            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(((Number) claims.get("userId")).longValue());
            loginUser.setUsername((String) claims.get("username"));
            loginUser.setRoleType(((Number) claims.get("roleType")).intValue());

            List<String> perms = extractPerms(claims);
            loginUser.setPerms(perms);

            Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
            for (String perm : perms) {
                authorities.add(new SimpleGrantedAuthority(perm));
            }
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            writeUnauthorized(response, "无效令牌");
        }
    }

    private List<String> extractPerms(Claims claims) {
        Object permsClaim = claims.get("perms");
        if (!(permsClaim instanceof List<?>)) {
            return new ArrayList<>();
        }

        List<String> perms = new ArrayList<>();
        for (Object value : (List<?>) permsClaim) {
            if (value instanceof String && StringUtils.hasText((String) value)) {
                perms.add((String) value);
            }
        }
        return perms;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(401, message)));
    }
}