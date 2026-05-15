package com.campus.seatreservation.security;

import com.campus.seatreservation.entity.User;
import com.campus.seatreservation.mapper.UserMapper;
import com.campus.seatreservation.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)throws ServletException, IOException {
        String token = getTokenFromRequest(request);
        log.info("请求路径: {}, 提取到的token: {}", request.getRequestURI(),
                token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "无");

        if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
            Long userId = jwtUtils.getUserIdFromToken(token);
            log.info("token有效, userId={}", userId);

            User user = userMapper.selectById(userId);
            if (user != null) {
                log.info("查到用户: {}", user.getUsername());
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("认证已设置到SecurityContext");
            } else {
                log.warn("userId={} 在数据库查不到用户", userId);
            }
        } else {
            log.info("token无效或为空, hasText={}, validate={}",
                    StringUtils.hasText(token),
                    StringUtils.hasText(token) ? jwtUtils.validateToken(token) : "跳过");
        }

        // 3. 放行（没 token 也放行，后续由 SecurityConfig 拦截未认证请求）
        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
