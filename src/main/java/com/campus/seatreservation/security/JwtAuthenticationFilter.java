package com.campus.seatreservation.security;

import com.campus.seatreservation.entity.User;
import com.campus.seatreservation.mapper.UserMapper;
import com.campus.seatreservation.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 *
 * 不标注 @Component，避免 Spring Boot 自动注册为 Servlet 过滤器导致执行两次。
 * 只在 SecurityConfig 里手动 new 并手动注册一次。
 *
 * 该过滤器在每次请求时提取JWT token，验证有效性，并将用户信息存入SecurityContext。
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    /**
     * 过滤请求，进行JWT认证
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
            // Token有效，解析用户信息
            Long userId = jwtUtils.getUserIdFromToken(token);
            User user = userMapper.selectById(userId);

            if (user != null) {
                // 构建认证对象，设置角色权限
                String role = jwtUtils.getRoleFromToken(token);
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.toUpperCase());
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user, null, List.of(authority));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                log.warn("userId={} 在数据库查不到用户", userId);
            }
        }

        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取JWT token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
