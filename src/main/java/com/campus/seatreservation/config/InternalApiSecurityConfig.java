package com.campus.seatreservation.config;

import com.campus.seatreservation.security.InternalTokenFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * 内部接口安全配置 —— 为 /internal/** 装配一条独立的过滤链。
 *
 * 与主链（SecurityConfig，负责 /api/** 的用户 JWT 鉴权）分离：
 * - 本链以 @Order(1) 优先匹配 /internal/**，用 X-Internal-Token 做服务间鉴权，
 *   供 Python Agent 以显式 userId 调用业务底座；
 * - 主链未加 securityMatcher，作为兜底继续处理其余请求（含 /api/**）。
 *
 * 注意：@EnableWebSecurity 已在 SecurityConfig 上开启，此处不重复标注，仅新增一条 SecurityFilterChain。
 */
@Configuration
public class InternalApiSecurityConfig {

    @Value("${internal.api.token:}")
    private String internalToken;

    @Bean
    @Order(1)
    public SecurityFilterChain internalFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // 鉴权实际由 InternalTokenFilter 完成：token 不匹配直接 401
                .addFilterBefore(new InternalTokenFilter(internalToken), AuthorizationFilter.class);
        return http.build();
    }
}
