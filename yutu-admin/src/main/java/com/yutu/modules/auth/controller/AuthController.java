package com.yutu.modules.auth.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.auth.dto.LoginRequest;
import com.yutu.modules.auth.dto.RegisterRequest;
import com.yutu.modules.auth.service.AuthService;
import com.yutu.modules.auth.vo.AuthLoginVO;
import com.yutu.modules.auth.vo.AuthMeVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<AuthLoginVO> login(@Validated @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/register")
    public Result<Long> register(@Validated @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @GetMapping("/me")
    public Result<AuthMeVO> me() {
        return Result.ok(authService.me());
    }
}
