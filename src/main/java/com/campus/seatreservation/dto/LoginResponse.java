package com.campus.seatreservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应 DTO — 登录成功后返回给前端的数据
 */
@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
}
