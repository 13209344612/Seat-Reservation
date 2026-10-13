package com.campus.seatreservation.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 内部接口鉴权过滤器 —— 校验服务间共享密钥头 X-Internal-Token。
 *
 * 仅作用于 /internal/**（由 InternalApiSecurityConfig 装配）。校验失败一律返回 401，
 * 采用「失败关闭」策略：未配置 token（空值）时拒绝所有内部请求，避免误开放。
 */
public class InternalTokenFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Token";

    private final String expectedToken;

    public InternalTokenFilter(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (expectedToken == null || expectedToken.isBlank() || !expectedToken.equals(provided)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"内部接口鉴权失败\",\"data\":null}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
