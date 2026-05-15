package com.campus.seatreservation.controller;

import com.campus.seatreservation.common.Result;
import com.campus.seatreservation.dto.LoginRequest;
import com.campus.seatreservation.dto.LoginResponse;
import com.campus.seatreservation.dto.RegisterRequest;
import com.campus.seatreservation.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {
    private final UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse data = userService.login(request);
        return Result.success(data);
    }
}
