package com.mortal.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortal.gateway.config.GatewayRateLimitProperties;
import com.mortal.gateway.util.ResponseUtil;
import com.mortal.platform.common.redis.PlatformRedisSupport;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class RedisLoginRateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RedisLoginRateLimitFilter.class);

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String KEY_DOMAIN = "rl";
    private static final String KEY_LOGIN = "login";
    private static final String KEY_IP = "ip";
    private static final String KEY_USER = "user";
    private static final String SCENE = "gateway-login-rate-limit";

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
    private final DefaultRedisScript<Long> fixedWindowRateLimitRedisScript;
    private final PlatformRedisSupport platformRedisSupport;
    private final GatewayRateLimitProperties rateLimitProperties;
    private final ResponseUtil responseUtil;
    private final ObjectMapper objectMapper;

    public RedisLoginRateLimitFilter(
        ReactiveStringRedisTemplate reactiveStringRedisTemplate,
        DefaultRedisScript<Long> fixedWindowRateLimitRedisScript,
        PlatformRedisSupport platformRedisSupport,
        GatewayRateLimitProperties rateLimitProperties,
        ResponseUtil responseUtil,
        ObjectMapper objectMapper
    ) {
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
        this.fixedWindowRateLimitRedisScript = fixedWindowRateLimitRedisScript;
        this.platformRedisSupport = platformRedisSupport;
        this.rateLimitProperties = rateLimitProperties;
        this.responseUtil = responseUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!shouldFilter(exchange)) {
            return chain.filter(exchange);
        }
        byte[] emptyBody = new byte[0];
        return DataBufferUtils.join(exchange.getRequest().getBody())
            .defaultIfEmpty(exchange.getResponse().bufferFactory().wrap(emptyBody))
            .flatMap(buffer -> {
                byte[] bodyBytes = new byte[buffer.readableByteCount()];
                buffer.read(bodyBytes);
                DataBufferUtils.release(buffer);

                String clientIp = resolveClientIp(exchange.getRequest());
                String username = extractUsername(bodyBytes);

                return enforceLimit(loginIpKey(clientIp),
                    rateLimitProperties.getLogin().getIpWindowSeconds(),
                    rateLimitProperties.getLogin().getIpMaxRequests())
                    .flatMap(ipAllowed -> {
                        if (!ipAllowed) {
                            log.warn("Gateway login rate limited by ip. ip={}", clientIp);
                            return reject(exchange);
                        }
                        if (!StringUtils.hasText(username)) {
                            return chain.filter(mutateExchange(exchange, bodyBytes));
                        }
                        return enforceLimit(loginUserKey(username),
                            rateLimitProperties.getLogin().getUserWindowSeconds(),
                            rateLimitProperties.getLogin().getUserMaxRequests())
                            .flatMap(userAllowed -> {
                                if (!userAllowed) {
                                    log.warn("Gateway login rate limited by username. username={}", username);
                                    return reject(exchange);
                                }
                                return chain.filter(mutateExchange(exchange, bodyBytes));
                            });
                    })
                    .onErrorResume(ex -> handleLimiterException(exchange, chain, bodyBytes, ex));
            });
    }

    @Override
    public int getOrder() {
        return -150;
    }

    private boolean shouldFilter(ServerWebExchange exchange) {
        if (!rateLimitProperties.getLogin().isEnabled()) {
            return false;
        }
        return HttpMethod.POST.equals(exchange.getRequest().getMethod())
            && LOGIN_PATH.equals(exchange.getRequest().getURI().getPath());
    }

    private Mono<Boolean> enforceLimit(String key, int windowSeconds, int maxRequests) {
        return reactiveStringRedisTemplate.execute(
                fixedWindowRateLimitRedisScript,
                List.of(key),
                List.of(String.valueOf(windowSeconds), String.valueOf(maxRequests)))
            .next()
            .doOnNext(result -> platformRedisSupport.recordRecovery(SCENE))
            .map(result -> Long.valueOf(1L).equals(result))
            .defaultIfEmpty(false);
    }

    private Mono<Void> handleLimiterException(ServerWebExchange exchange,
                                              GatewayFilterChain chain,
                                              byte[] bodyBytes,
                                              Throwable ex) {
        if (rateLimitProperties.getLogin().isFailOpen()) {
            platformRedisSupport.recordFailure(SCENE, ex instanceof Exception exception ? exception : new IllegalStateException(ex.getMessage(), ex));
            log.error("Gateway login rate limiter degraded to fail-open.", ex);
            return chain.filter(mutateExchange(exchange, bodyBytes));
        }
        platformRedisSupport.recordFailure(SCENE, ex instanceof Exception exception ? exception : new IllegalStateException(ex.getMessage(), ex));
        log.error("Gateway login rate limiter rejected because Redis is unavailable.", ex);
        return responseUtil.writeJson(
            exchange,
            HttpStatus.TOO_MANY_REQUESTS,
            429,
            "login rate limiter unavailable",
            traceId(exchange)
        );
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        return responseUtil.writeJson(
            exchange,
            HttpStatus.TOO_MANY_REQUESTS,
            429,
            "login requests are too frequent",
            traceId(exchange)
        );
    }

    private ServerWebExchange mutateExchange(ServerWebExchange exchange, byte[] bodyBytes) {
        ServerHttpRequestDecorator decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(super.getHeaders());
                headers.remove(HttpHeaders.CONTENT_LENGTH);
                headers.setContentLength(bodyBytes.length);
                return headers;
            }

            @Override
            public Flux<DataBuffer> getBody() {
                return Flux.defer(() -> Mono.just(exchange.getResponse().bufferFactory().wrap(bodyBytes)));
            }
        };
        return exchange.mutate().request(decoratedRequest).build();
    }

    private String extractUsername(byte[] bodyBytes) {
        if (bodyBytes == null || bodyBytes.length == 0) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(new String(bodyBytes, StandardCharsets.UTF_8));
            String username = root.path("username").asText(null);
            if (!StringUtils.hasText(username)) {
                return null;
            }
            return username.trim().toLowerCase(Locale.ROOT);
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolveClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return sanitizeDimension(xForwardedFor.split(",")[0].trim());
        }
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return sanitizeDimension(xRealIp.trim());
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return "unknown";
        }
        return sanitizeDimension(remoteAddress.getAddress().getHostAddress());
    }

    private String loginIpKey(String ip) {
        return key(KEY_DOMAIN, KEY_LOGIN, KEY_IP, ip);
    }

    private String loginUserKey(String username) {
        return key(KEY_DOMAIN, KEY_LOGIN, KEY_USER, sanitizeDimension(username));
    }

    private String key(String... segments) {
        return platformRedisSupport.buildKey(segments);
    }

    private String sanitizeDimension(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "unknown";
        }
        return raw.trim().replace(":", "_").replace(" ", "_");
    }
    private String traceId(ServerWebExchange exchange) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER);
        return traceId == null ? "" : traceId;
    }
}
