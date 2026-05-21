package com.campus.seatreservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.seatreservation.dto.LoginRequest;
import com.campus.seatreservation.dto.LoginResponse;
import com.campus.seatreservation.dto.RegisterRequest;
import com.campus.seatreservation.entity.User;
import com.campus.seatreservation.mapper.UserMapper;
import com.campus.seatreservation.service.UserService;
import com.campus.seatreservation.util.JwtUtils;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户业务层实现
 *
 * 处理用户注册和登录的业务逻辑，包括密码加密和JWT token生成。
 */
@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    /**
     * 用户注册
     */
    @Override
    public void register(RegisterRequest request) {
        // 1. 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User existUser = userMapper.selectOne(wrapper);

        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 2. 创建新用户，密码使用BCrypt加密
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("student");  // 默认角色为学生

        userMapper.insert(user);
    }

    /**
     * 用户登录
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 2. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. 生成JWT token
        String token = jwtUtils.generateToken(user.getId(), user.getRole());
        return new LoginResponse(token, user.getId(), user.getUsername());
    }
}
