package com.a05.admin.util;

import com.a05.admin.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类：提供 Token 生成与解析功能
 */
@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 生成 JWT Token
     *
     * @param subject 主题（通常为 userId）
     * @param claims  额外的 Claims（如 role 等）
     * @return JWT Token 字符串
     */
    public String generateToken(String subject, Map<String, Object> claims) {
        long expireSeconds = jwtProperties.getExpireSeconds();
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expireSeconds);

        return Jwts.builder()
                .setSubject(subject)
                .addClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(secretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析并校验 Token；Token 非法或过期时抛出 JwtException
     *
     * @param token JWT Token 字符串
     * @return Claims（包含 subject、过期时间等）
     */
    public Claims parseAndValidate(String token) throws JwtException {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(secretKey())
                .build()
                .parseClaimsJws(token);
        return jws.getBody();
    }

    private SecretKey secretKey() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret is blank");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
