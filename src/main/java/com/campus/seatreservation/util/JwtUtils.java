package com.campus.seatreservation.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * JWT 工具类 — 负责 token 的生成、解析、校验
 * 注意：载荷是 Base64 编码，不是加密，不能放密码等敏感信息。
 */
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;  // JWT签名密钥（Base64编码）

    @Value("${jwt.expiration}")
    private long expiration;  // Token有效期（毫秒）

    /**
     * 获取签名密钥
     */
    private SecretKey getKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成JWT Token
     */
    public String generateToken(Long userId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))  // 主题：用户ID
                .claim("role", role)  // 自定义声明：角色
                .issuedAt(now)  // 签发时间
                .expiration(expiryDate)  // 过期时间
                .signWith(getKey())  // 签名
                .compact();
    }

    /**
     * 从Token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 从Token中获取用户角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("role", String.class);
    }

    /**
     * 校验 token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
