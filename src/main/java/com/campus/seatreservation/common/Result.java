package com.campus.seatreservation.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应类 — 所有 Controller 返回给前端的数据都用这个格式包裹
 */
@Data
@AllArgsConstructor
@NoArgsConstructor  // Jackson 反序列化 JSON 时需要无参构造
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> success(String message) {
        return new Result<>(200, message, null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
