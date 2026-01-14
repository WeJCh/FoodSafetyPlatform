package com.mortal.user.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TokenUtil {

    private final Set<String> invalidTokens = ConcurrentHashMap.newKeySet();

    private String secret;
    private long expirationMinutes;

    @Value("${jwt.secret}")  // 从配置文件中读取，如 application.properties/yml
    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Value("${jwt.expiration-minutes}")  // 默认120分钟
    public void setExpirationMinutes(long expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(Long userId, String username, String userType) {
        Instant now = Instant.now();
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("userType", userType)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean verify(String token) {
        String normalized = normalize(token);
        if (!StringUtils.hasText(normalized) || invalidTokens.contains(normalized)) {
            return false;
        }
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(normalized);
            return true;
        } catch (JwtException ex) {
            return false;
        }
    }

    public Claims parseToken(String token) {
        String normalized = normalize(token);
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(normalized)
                .getBody();
    }

    public Long getUserId(String token) {
        try {
            Claims claims = parseToken(token);
            String subject = claims.getSubject();
            return subject == null ? null : Long.valueOf(subject);
        } catch (JwtException | NumberFormatException ex) {
            return null;
        }
    }

    public void invalidate(String token) {
        String normalized = normalize(token);
        if (StringUtils.hasText(normalized)) {
            invalidTokens.add(normalized);
        }
    }

    private String normalize(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        if (token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }
}
