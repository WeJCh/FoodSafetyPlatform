package com.mortal.gateway.filter;

import com.mortal.gateway.util.ResponseUtil;
import com.mortal.gateway.vo.AuthIntrospectVO;
import com.mortal.platform.common.ApiResponse;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<PathRule> WHITELIST = List.of(
        PathRule.exact("/api/auth/login"),
        PathRule.exact("/api/auth/verify"),
        PathRule.exact("/api/users/register/public"),
        PathRule.exact("/api/users/register/enterprise"),
        PathRule.exact("/api/health"),
        PathRule.exact("/actuator/health")
    );

    private static final List<AccessRule> ROLE_RULES = List.of(
        AccessRule.roles(PathRule.prefix("/api/admin/"), "ADMIN"),
        AccessRule.roles(PathRule.prefix("/api/roles/"), "ADMIN"),
        AccessRule.authenticated(PathRule.exact("/api/users/me")),
        AccessRule.authenticated(PathRule.exact("/api/users/me/password")),
        AccessRule.roles(PathRule.pattern("/api/users/*"), "ADMIN"),
        AccessRule.roles(PathRule.prefix("/api/files/"), "PUBLIC", "ENTERPRISE"),
        AccessRule.roles(PathRule.exact("/api/complaints/public"), "PUBLIC"),
        AccessRule.roles(PathRule.prefix("/api/complaints/my"), "PUBLIC"),
        AccessRule.roles(PathRule.prefix("/api/complaints/"), "REGULATOR_ADMIN", "REGULATOR_ENFORCER"),
        AccessRule.roles(PathRule.prefix("/api/regulation/public/"), "PUBLIC"),
        AccessRule.roles(PathRule.prefix("/api/regulation-operation/public/sampling/results"), "PUBLIC"),
        AccessRule.roles(PathRule.prefix("/api/regulation-operation/rectifications/my"), "ENTERPRISE"),
        AccessRule.roles(PathRule.prefix("/api/regulation-operation/rectifications/"), "ENTERPRISE", "REGULATOR_ADMIN", "REGULATOR_ENFORCER"),
        AccessRule.roles(PathRule.prefix("/api/regulation-operation/inspections/enterprise"), "ENTERPRISE"),
        AccessRule.roles(PathRule.prefix("/api/regulation-operation/"), "REGULATOR_ADMIN", "REGULATOR_ENFORCER"),
        AccessRule.roles(PathRule.prefix("/api/regulation/enterprise/"), "ENTERPRISE", "REGULATOR_ADMIN", "REGULATOR_ENFORCER"),
        AccessRule.roles(PathRule.prefix("/api/regulation/products"), "ENTERPRISE"),
        AccessRule.roles(PathRule.prefix("/api/regulation/regions"), "ADMIN", "ENTERPRISE", "REGULATOR_ADMIN", "REGULATOR_ENFORCER"),
        AccessRule.roles(PathRule.prefix("/api/regulation/regulators"), "ADMIN", "REGULATOR_ADMIN", "REGULATOR_ENFORCER"),
        AccessRule.roles(PathRule.prefix("/api/regulation/"), "REGULATOR_ADMIN", "REGULATOR_ENFORCER"),
        AccessRule.roles(PathRule.prefix("/api/query/"), "ADMIN", "REGULATOR_ADMIN", "REGULATOR_ENFORCER")
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

    @Value("${gateway.auth.introspect-fail-open:false}")
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
        if (path.startsWith("/api/warning/")) {
            return forbidden(exchange);
        }

        String token = extractToken(exchange.getRequest().getHeaders());
        if (!isValidSignature(token)) {
            return unauthorized(exchange);
        }

        return introspect(token)
            .flatMap(identity -> {
                if (!isActiveIdentity(identity)) {
                    if (identity == null && failOpen) {
                        log.warn("Gateway introspect unavailable, fail-open enabled. path={}", path);
                        return chain.filter(exchange);
                    }
                    log.warn(
                        "Gateway introspect failed. path={}, userId={}, valid={}, status={}, deleted={}",
                        path,
                        identity == null ? null : identity.getUserId(),
                        identity == null ? null : identity.isValid(),
                        identity == null ? null : identity.getStatus(),
                        identity == null ? null : identity.getDeleted()
                    );
                    return unauthorized(exchange);
                }

                if (!isAllowedByRole(path, identity.getRoles(), identity.getUserType())) {
                    return forbidden(exchange);
                }

                log.info(
                    "Gateway auth pass. path={}, userId={}, roles={}",
                    path,
                    identity.getUserId(),
                    joinRoles(identity.getRoles())
                );

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
                if (failOpen) {
                    log.warn("Gateway introspect call exception, fail-open enabled. path={}", path, ex);
                    return chain.filter(exchange);
                }
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
        return WHITELIST.stream().anyMatch(rule -> rule.matches(path));
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
        for (AccessRule rule : ROLE_RULES) {
            if (rule.matches(path)) {
                return rule.allows(effectiveRoles);
            }
        }
        return true;
    }

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

    private record AccessRule(PathRule pathRule, boolean authenticatedOnly, List<String> roles) {

        static AccessRule authenticated(PathRule pathRule) {
            return new AccessRule(pathRule, true, List.of());
        }

        static AccessRule roles(PathRule pathRule, String... roles) {
            return new AccessRule(pathRule, false, List.of(roles));
        }

        boolean matches(String path) {
            return pathRule.matches(path);
        }

        boolean allows(List<String> userRoles) {
            if (authenticatedOnly) {
                return true;
            }
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

    private record PathRule(MatchType matchType, String pattern) {

        static PathRule exact(String pattern) {
            return new PathRule(MatchType.EXACT, pattern);
        }

        static PathRule prefix(String pattern) {
            return new PathRule(MatchType.PREFIX, pattern);
        }

        static PathRule pattern(String pattern) {
            return new PathRule(MatchType.PATTERN, pattern);
        }

        boolean matches(String path) {
            if (path == null) {
                return false;
            }
            return switch (matchType) {
                case EXACT -> path.equals(pattern);
                case PREFIX -> path.startsWith(pattern);
                case PATTERN -> PATH_MATCHER.match(pattern, path);
            };
        }
    }

    private enum MatchType {
        EXACT,
        PREFIX,
        PATTERN
    }
}
