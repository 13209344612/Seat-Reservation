package com.campus.seatreservation.controller;

import com.campus.seatreservation.common.Result;
import com.campus.seatreservation.dto.LoginRequest;
import com.campus.seatreservation.dto.LoginResponse;
import com.campus.seatreservation.dto.RegisterRequest;
import com.campus.seatreservation.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 — 对外暴露注册和登录两个接口
 */
@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * POST /api/auth/register
     * 请求体：{ "username": "zhangsan", "password": "123456" }
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success("注册成功");
    }

    /**
     * POST /api/auth/login
     * 请求体：{ "username": "zhangsan", "password": "123456" }
     *
     * 前端拿到 token 后存 localStorage，后续请求带在 Authorization Header 里。
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse data = userService.login(request);
        return Result.success(data);
    }
}