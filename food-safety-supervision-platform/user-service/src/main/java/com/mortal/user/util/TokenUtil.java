package com.mortal.user.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
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

    @Value("${jwt.secret}")
    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Value("${jwt.expiration-minutes}")
    public void setExpirationMinutes(long expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(Long userId, String username, String userType, java.util.List<String> roles) {
        Instant now = Instant.now();
        Instant expireAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
            .setSubject(String.valueOf(userId))
            .setId(generateJti())
            .claim("username", username)
            .claim("userType", userType)
            .claim("roles", roles == null ? java.util.List.of() : roles)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expireAt))
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

    public String getJti(String token) {
        try {
            return parseToken(token).getId();
        } catch (JwtException ex) {
            return null;
        }
    }

    public Instant getExpireAt(String token) {
        try {
            Date expiration = parseToken(token).getExpiration();
            return expiration == null ? null : expiration.toInstant();
        } catch (JwtException ex) {
            return null;
        }
    }

    public long getRemainingSeconds(String token) {
        Instant expireAt = getExpireAt(token);
        if (expireAt == null) {
            return 0L;
        }
        long seconds = Instant.now().until(expireAt, ChronoUnit.SECONDS);
        return Math.max(seconds, 0L);
    }

    public String getTokenHash(String token) {
        String normalized = normalize(token);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
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

    private String generateJti() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
