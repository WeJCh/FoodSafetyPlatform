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
        "/api/health",
        "/actuator/health"
    );

    private static final List<RoleRule> ROLE_RULES = List.of(
        RoleRule.of("/api/admin/", "ADMIN"),
        // File upload presign is used by public complaints and enterprise rectification.
        RoleRule.of("/api/files/", "PUBLIC", "ENTERPRISE"),
        RoleRule.of("/api/regulation/public/", "PUBLIC"),
        RoleRule.of("/api/regulation/complaints/public", "PUBLIC"),
        RoleRule.of("/api/regulation/complaints/my", "PUBLIC"),
        // Enterprise rectification endpoints for enterprise users.
        RoleRule.of("/api/regulation/rectifications/my", "ENTERPRISE"),
        // Shared rectification detail/action endpoints for enterprise and regulators.
        RoleRule.of("/api/regulation/rectifications/", "ENTERPRISE", "REGULATOR_ADMIN", "REGULATOR_ENFORCER"),
        // Enterprise profile and filing endpoints.
        RoleRule.of("/api/regulation/enterprise/", "ENTERPRISE", "REGULATOR_ADMIN", "REGULATOR_ENFORCER"),
        // Region tree query for enterprise filing forms and regulators.
        RoleRule.of("/api/regulation/regions", "ENTERPRISE", "REGULATOR_ADMIN", "REGULATOR_ENFORCER"),
        RoleRule.of("/api/regulation/", "REGULATOR_ADMIN", "REGULATOR_ENFORCER"),
        RoleRule.of("/api/query/", "ADMIN", "REGULATOR_ADMIN", "REGULATOR_ENFORCER"),
        RoleRule.of("/api/warning/", "ADMIN", "REGULATOR_ADMIN", "REGULATOR_ENFORCER")
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

    /**
     * 鏉╁洦鎶?     * @param exchange 娴溿倖宕查張?     * @param chain 闁?     * @return 缁?     */
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

                if (!isAllowedByRole(path, identity.getRoles(), identity.getUserType())) {
                    return forbidden(exchange);
                }

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

    /**
     * 閺勵垰鎯佹０鍕梾
     * @param exchange 娴溿倖宕查張?     * @return 閺勵垰鎯佹０鍕梾
     */
    private boolean isPreflight(ServerWebExchange exchange) {
        return HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod());
    }

    /**
     * 閺勵垰鎯侀惂钘夋倳閸?     * @param path 鐠侯垰绶?     * @return 閺勵垰鎯侀惂钘夋倳閸?     */
    private boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(path::startsWith);
    }

    /**
     * 閹绘劕褰囨禒銈囧
     * @param headers 婢?     * @return 娴犮倗澧?     */
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

    /**
     * 閺勵垰鎯侀張澶嬫櫏缁涙儳鎮?     * @param token 娴犮倗澧?     * @return 閺勵垰鎯侀張澶嬫櫏
     */
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

    /**
     *  introspect
     * @param token 娴犮倗澧?     * @return 闊偂鍞?     */
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

    /**
     * 閺勵垰鎯佸┑鈧ú鏄忛煩娴?     * @param identity 闊偂鍞?     * @return 閺勵垰鎯佸┑鈧ú?     */
    private boolean isActiveIdentity(AuthIntrospectVO identity) {
        if (identity == null || !identity.isValid() || identity.getUserId() == null) {
            return false;
        }
        boolean enabled = identity.getStatus() == null || Objects.equals(identity.getStatus(), 1);
        boolean notDeleted = identity.getDeleted() == null || !Objects.equals(identity.getDeleted(), 1);
        return enabled && notDeleted;
    }

    /**
     * 閹峰吋甯寸憴鎺曞
     * @param roles 鐟欐帟澹?     * @return 閹峰吋甯撮崥搴ｆ畱鐟欐帟澹?     */
    private String joinRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "";
        }
        return roles.stream()
            .filter(StringUtils::hasText)
            .collect(Collectors.joining(","));
    }

    /**
     * 姒涙顓荤€涙顑佹稉?     * @param value 閸?     * @return 姒涙顓荤€涙顑佹稉?     */
    private String defaultString(String value) {
        return StringUtils.hasText(value) ? value : "";
    }

    /**
     * 閺堫亝宸块弶?     * @param exchange 娴溿倖宕查張?     * @return 閺堫亝宸块弶?     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
            return responseUtil.writeJson(
            exchange,
            HttpStatus.UNAUTHORIZED,
            401,
            "unauthorized",
            getTraceId(exchange)
        );
    }

    /**
     * 閺勵垰鎯侀崗浣筋啅鐠佸潡妫?     * @param path 鐠侯垰绶?     * @param roles 閻劍鍩涚憴鎺曞
     * @param userType 閻劍鍩涚猾璇茬€?     * @return 閺勵垰鎯侀崗浣筋啅鐠佸潡妫?     */
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
     * 鐞涖儱鍘栫憴鎺曞
     * @param roles 閻劍鍩涚憴鎺曞
     * @param userType 閻劍鍩涚猾璇茬€?     * @return 鐞涖儱鍘栭崥搴ｆ畱鐟欐帟澹?     */
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
    /**
     * 缁備焦顒涚拋鍧楁６
     * @param exchange 娴溿倖宕查張?     * @return 缁備焦顒涚拋鍧楁６
     */
    private Mono<Void> forbidden(ServerWebExchange exchange) {
        return responseUtil.writeJson(
            exchange,
            HttpStatus.FORBIDDEN,
            403,
            "forbidden",
            getTraceId(exchange)
        );
    }

    /**
     * 閼惧嘲褰囨潻鍊熼嚋ID
     * @param exchange 娴溿倖宕查張?     * @return 鏉╁€熼嚋ID
     */
    private String getTraceId(ServerWebExchange exchange) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER);
        return traceId == null ? "" : traceId;
    }

    /**
     * 鐟欐帟澹婄憴鍕灟
     * @param pathPrefix 鐠侯垰绶為崜宥囩磻
     * @param roles 鐟欐帟澹?     */
    private record RoleRule(String pathPrefix, List<String> roles) {

        static RoleRule of(String pathPrefix, String... roles) {
            return new RoleRule(pathPrefix, List.of(roles));
        }

        /**
         * 閸栧綊鍘ょ憴鎺曞
         * @param userRoles 閻劍鍩涚憴鎺曞
         * @return 閺勵垰鎯侀崠褰掑帳
         */
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
