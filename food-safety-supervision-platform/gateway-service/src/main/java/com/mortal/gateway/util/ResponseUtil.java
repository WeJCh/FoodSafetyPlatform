package com.mortal.gateway.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ResponseUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Mono<Void> writeJson(ServerWebExchange exchange,
                               HttpStatus status,
                               int code,
                               String message,
                               String traceId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", code);
        payload.put("message", message);
        payload.put("traceId", traceId == null ? "" : traceId);
        payload.put("timestamp", Instant.now().toString());
        byte[] body = toJsonBytes(payload);
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    private byte[] toJsonBytes(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            String fallback = "{\"code\":500,\"message\":\"internal error\",\"traceId\":\"\",\"timestamp\":\"\"}";
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }
}

