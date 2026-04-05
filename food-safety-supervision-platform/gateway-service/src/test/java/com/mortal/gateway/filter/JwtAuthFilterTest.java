package com.mortal.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mortal.gateway.util.ResponseUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class JwtAuthFilterTest {

    private static final String SECRET = "foodUserServiceJwtSecretKey123456";

    @Test
    void filter_shouldRejectProtectedRequestWhenIntrospectFailsAndFailOpenDisabled() {
        JwtAuthFilter filter = newFilter(false, request -> Mono.error(new IllegalStateException("user-service down")));
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        GatewayFilterChain chain = exchange -> {
            chainInvoked.set(true);
            return Mono.empty();
        };
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/regulation/enterprise/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .build()
        );

        filter.filter(exchange, chain).block();

        assertFalse(chainInvoked.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertTrue(body != null && body.contains("\"code\":401"));
        assertTrue(body != null && body.contains("\"message\":\"unauthorized\""));
    }

    @Test
    void filter_shouldAllowProtectedRequestWhenIntrospectFailsAndFailOpenEnabled() {
        JwtAuthFilter filter = newFilter(true, request -> Mono.error(new IllegalStateException("user-service down")));
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        GatewayFilterChain chain = exchange -> {
            chainInvoked.set(true);
            return Mono.empty();
        };
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/regulation/enterprise/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .build()
        );

        filter.filter(exchange, chain).block();

        assertTrue(chainInvoked.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_shouldAllowPublicSamplingResultRequestForPublicUser() {
        JwtAuthFilter filter = newFilter(false, request -> Mono.just(successResponse("PUBLIC", List.of("PUBLIC"))));
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        GatewayFilterChain chain = exchange -> {
            chainInvoked.set(true);
            return Mono.empty();
        };
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/regulation-operation/public/sampling/results")
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .build()
        );

        filter.filter(exchange, chain).block();

        assertTrue(chainInvoked.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_shouldAllowEnterpriseToCreateProduct() {
        JwtAuthFilter filter = newFilter(false, request -> Mono.just(successResponse("ENTERPRISE", List.of("ENTERPRISE"))));
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        GatewayFilterChain chain = exchange -> {
            chainInvoked.set(true);
            return Mono.empty();
        };
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/regulation/products")
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"test-product\"}")
        );

        filter.filter(exchange, chain).block();

        assertTrue(chainInvoked.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    private JwtAuthFilter newFilter(boolean failOpen, ExchangeFunction exchangeFunction) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        JwtAuthFilter filter = new JwtAuthFilter(builder, new ResponseUtil());
        filter.setSecret(SECRET);
        filter.setFailOpen(failOpen);
        filter.setIntrospectTimeoutMs(1000);
        return filter;
    }

    private ClientResponse successResponse(String userType, List<String> roles) {
        String roleJson = roles.stream()
            .map(role -> "\"" + role + "\"")
            .reduce((left, right) -> left + "," + right)
            .orElse("");
        String body = """
            {
              "code":0,
              "message":"ok",
              "data":{
                "valid":true,
                "userId":18,
                "username":"public-user",
                "userType":"%s",
                "status":1,
                "deleted":0,
                "roles":[%s]
              }
            }
            """.formatted(userType, roleJson);
        return ClientResponse.create(HttpStatus.OK)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(body)
            .build();
    }

    private String bearerToken() {
        String token = Jwts.builder()
            .setSubject("18")
            .claim("userType", "REGULATOR")
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();
        return "Bearer " + token;
    }
}
