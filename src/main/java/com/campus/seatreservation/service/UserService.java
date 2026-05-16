package com.campus.seatreservation.service;

import com.campus.seatreservation.dto.LoginRequest;
import com.campus.seatreservation.dto.LoginResponse;
import com.campus.seatreservation.dto.RegisterRequest;

/**
 * 用户业务层接口
 */
public interface UserService {

    void register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
