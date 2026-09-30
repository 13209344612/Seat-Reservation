package com.campus.seatreservation.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理系统中抛出的异常，返回标准化的错误响应。
 * 防止敏感信息泄露，提供友好的错误提示。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理权限不足异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        return Result.error(403, "权限不足");
    }

    /**
     * 处理参数校验异常（@Valid）
     * 防止校验失败时返回 500 错误和堆栈信息，暴露系统细节
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.error(400, message);
    }

    /**
     * 处理业务运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        return Result.error(400, e.getMessage());
    }

    /**
     * 兜底处理其余未捕获异常（含 AI 调用超时/限流、第三方 SDK 异常等）。
     * 记录日志便于排查，对外仅返回通用提示，避免泄露内部堆栈与敏感细节。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return Result.error(500, "服务器繁忙，请稍后重试");
    }
}
