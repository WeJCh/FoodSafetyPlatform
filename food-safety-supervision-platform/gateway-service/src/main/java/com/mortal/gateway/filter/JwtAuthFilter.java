package com.mortal.gateway.filter;

import com.mortal.gateway.common.ApiResponse;
import com.mortal.gateway.util.ResponseUtil;
import com.mortal.gateway.filter.TraceIdFilter;
import com.mortal.gateway.vo.AuthIntrospectVO;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final List<String> WHITELIST = List.of(
        "/api/auth/login",
        "/api/auth/verify",
        "/api/users/register",
        "/api/users/register/public",
        "/api/users/register/enterprise",
        "/api/regulation/complaints/public",
        "/api/regulation/complaints/track",
        "/api/health",
        "/actuator/health"
    );

    private static final List<RoleRule> ROLE_RULES = List.of(
        RoleRule.of("/api/admin/", "ADMIN"),
        RoleRule.of("/api/regulation/public/", "PUBLIC"),
        // 关键注释：企业备案/查看属于企业用户能力，需要在网关放行 ENTERPRISE
        RoleRule.of("/api/regulation/enterprise/", "ENTERPRISE", "REGULATOR_ADMIN", "REGULATOR_ENFORCER"),
        // 关键注释：行政区联动查询需对企业用户开放，用于备案表单的省市区街道选择
        RoleRule.of("/api/regulation/regions", "ENTERPRISE", "REGULATOR_ADMIN", "REGULATOR_ENFORCER"),
        RoleRule.of("/api/regulation/", "REGULATOR_ADMIN", "REGULATOR_ENFORCER"),
        RoleRule.of("/api/query/", "ADMIN", "REGULATOR_ADMIN", "REGULATOR_ENFORCER"),
        RoleRule.of("/api/warning/", "ADMIN", "REGULATOR_ADMIN", "REGULATOR_ENFORCER"),
        RoleRule.of("/api/regulation/complaints/public", "PUBLIC"),
        RoleRule.of("/api/regulation/complaints/track", "PUBLIC")
    );

    private final WebClient webClient;
    private final ResponseUtil responseUtil;

    private String secret;
    private boolean failOpen;
    private long introspectTimeoutMs;

    public JwtAuthFilter(WebClient.Builder webClientBuilder, ResponseUtil responseUtil) {
        this.webClient = webClientBuilder.build();
        this.responseUtil = responseUtil;
    }

    @Value("${jwt.secret}")
    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Value("${gateway.auth.introspect-fail-open:true}")
    public void setFailOpen(boolean failOpen) {
        this.failOpen = failOpen;
    }

    @Value("${gateway.auth.introspect-timeout-ms:1500}")
    public void setIntrospectTimeoutMs(long introspectTimeoutMs) {
        this.introspectTimeoutMs = introspectTimeoutMs;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPreflight(exchange) || isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange.getRequest().getHeaders());
        if (!isValidSignature(token)) {
            return unauthorized(exchange);
        }

        return introspect(token)
            .flatMap(identity -> {
                if (!isActiveIdentity(identity)) {
                    if (identity == null && failOpen) {
                        // Fail-open: allow request when user-service is unavailable to avoid system-wide 401.
                        log.warn("Gateway introspect unavailable, fail-open enabled. path={}", path);
                        return chain.filter(exchange);
                    }
                    log.warn("Gateway introspect failed. path={}, userId={}, valid={}, status={}, deleted={}",
                        path,
                        identity == null ? null : identity.getUserId(),
                        identity == null ? null : identity.isValid(),
                        identity == null ? null : identity.getStatus(),
                        identity == null ? null : identity.getDeleted());
                    return unauthorized(exchange);
                }

                // 关键注释：根据用户类型和角色判断是否允许访问
                if (!isAllowedByRole(path, identity.getRoles(), identity.getUserType())) {
                    return forbidden(exchange);
                }

                // 关键注释：在网关日志中记录当前用户角色，便于排查权限问题
                log.info("Gateway auth pass. path={}, userId={}, roles={}",
                    path,
                    identity.getUserId(),
                    joinRoles(identity.getRoles()));

                ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                        .header("X-User-Id", String.valueOf(identity.getUserId()))
                        .header("X-Username", defaultString(identity.getUsername()))
                        .header("X-User-Type", defaultString(identity.getUserType()))
                        .header("X-User-Roles", joinRoles(identity.getRoles()))
                        .build())
                    .build();
                return chain.filter(mutatedExchange);
            })
            .onErrorResume(ex -> {
                log.error("Gateway introspect call exception.", ex);
                return unauthorized(exchange);
            });
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isPreflight(ServerWebExchange exchange) {
        return HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod());
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(path::startsWith);
    }

    private String extractToken(HttpHeaders headers) {
        String header = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header)) {
            return null;
        }
        if (header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return header;
    }

    private boolean isValidSignature(String token) {
        if (!StringUtils.hasText(token) || !StringUtils.hasText(secret)) {
            return false;
        }
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException ex) {
            return false;
        }
    }

    private Mono<AuthIntrospectVO> introspect(String token) {
        return webClient.post()
            .uri("http://user-service/api/auth/introspect")
            .header(HttpHeaders.AUTHORIZATION, token)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<AuthIntrospectVO>>() {})
            // Timeout protects gateway from hanging when user-service is slow or unavailable.
            .timeout(Duration.ofMillis(introspectTimeoutMs))
            .map(response -> {
                if (response == null || !response.isSuccess()) {
                    log.warn("Gateway introspect call failed. response={}", response);
                    return null;
                }
                return response.getData();
            });
    }

    private boolean isActiveIdentity(AuthIntrospectVO identity) {
        if (identity == null || !identity.isValid() || identity.getUserId() == null) {
            return false;
        }
        boolean enabled = identity.getStatus() == null || Objects.equals(identity.getStatus(), 1);
        boolean notDeleted = identity.getDeleted() == null || !Objects.equals(identity.getDeleted(), 1);
        return enabled && notDeleted;
    }

    private String joinRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "";
        }
        return roles.stream()
            .filter(StringUtils::hasText)
            .collect(Collectors.joining(","));
    }

    private String defaultString(String value) {
        return StringUtils.hasText(value) ? value : "";
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        // 关键注释：统一网关错误返回结构，便于前端统一处理与日志追踪
        return responseUtil.writeJson(
            exchange,
            HttpStatus.UNAUTHORIZED,
            401,
            "unauthorized",
            getTraceId(exchange)
        );
    }

    private boolean isAllowedByRole(String path, List<String> roles, String userType) {
        List<String> effectiveRoles = enrichRoles(roles, userType);
        for (RoleRule rule : ROLE_RULES) {
            if (path.startsWith(rule.pathPrefix())) {
                return rule.matches(effectiveRoles);
            }
        }
        return true;
    }

    /**
     * 关键注释：根据用户类型和角色判断是否允许访问
     * @param roles 用户角色
     * @param userType 用户类型
     * @return 增强后的角色列表
     */
    private List<String> enrichRoles(List<String> roles, String userType) {
        List<String> result = new ArrayList<>();
        if (roles != null && !roles.isEmpty()) {
            result.addAll(roles);
        }
        if (StringUtils.hasText(userType) && !result.contains(userType)) {
            result.add(userType);
        }
        return result;
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        // 关键注释：无权限访问时返回统一结构，避免前端解析分支
        return responseUtil.writeJson(
            exchange,
            HttpStatus.FORBIDDEN,
            403,
            "forbidden",
            getTraceId(exchange)
        );
    }

    private String getTraceId(ServerWebExchange exchange) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER);
        return traceId == null ? "" : traceId;
    }

    private record RoleRule(String pathPrefix, List<String> roles) {

        static RoleRule of(String pathPrefix, String... roles) {
            return new RoleRule(pathPrefix, List.of(roles));
        }

        boolean matches(List<String> userRoles) {
            if (userRoles == null || userRoles.isEmpty()) {
                return false;
            }
            for (String role : userRoles) {
                if (roles.contains(role)) {
                    return true;
                }
            }
            return false;
        }
    }
}
