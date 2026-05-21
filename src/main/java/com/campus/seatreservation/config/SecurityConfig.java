package com.campus.seatreservation.config;

import com.campus.seatreservation.mapper.UserMapper;
import com.campus.seatreservation.security.JwtAuthenticationFilter;
import com.campus.seatreservation.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * Spring Security 安全配置类
 *
 * 配置JWT认证过滤器、权限控制规则和密码编码器。
 * 采用无状态会话管理，所有请求都需要通过JWT token进行认证。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // 启用方法级权限控制
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    /**
     * 配置安全过滤链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 创建JWT认证过滤器实例（不标注@Component避免重复注册）
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtils, userMapper);

        http
                .csrf(AbstractHttpConfigurer::disable)  // 禁用CSRF（无状态API不需要）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // 无状态会话
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()  // 认证接口放行
                        .anyRequest().authenticated()  // 其他接口需要认证
                )
                .addFilterBefore(jwtFilter, AuthorizationFilter.class);  // 在授权过滤器前添加JWT过滤器

        return http.build();
    }

    /**
     * 配置密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
