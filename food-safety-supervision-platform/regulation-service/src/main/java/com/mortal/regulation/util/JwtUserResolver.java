package com.mortal.regulation.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtUserResolver {

    private String secret;

    @Value("${jwt.secret}")
    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Long resolveUserId(String token) {
        Claims claims = parse(token);
        if (claims == null) {
            return null;
        }
        String subject = claims.getSubject();
        if (!StringUtils.hasText(subject)) {
            return null;
        }
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public String resolveUserType(String token) {
        Claims claims = parse(token);
        if (claims == null) {
            return null;
        }
        Object value = claims.get("userType");
        return value == null ? null : String.valueOf(value);
    }

    public String resolveUsername(String token) {
        Claims claims = parse(token);
        if (claims == null) {
            return null;
        }
        Object value = claims.get("username");
        return value == null ? null : String.valueOf(value);
    }

    private Claims parse(String token) {
        String normalized = normalize(token);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(normalized)
                .getBody();
        } catch (JwtException ex) {
            return null;
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
