package com.mortal.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import com.mortal.gateway.filter.TraceIdFilter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Configuration
public class SentinelGatewayBlockHandler {

    @PostConstruct
    public void initBlockHandler() {
        GatewayCallbackManager.setBlockHandler(new JsonBlockRequestHandler());
    }

    private class JsonBlockRequestHandler implements BlockRequestHandler {

        @Override
        public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable ex) {
            String traceId = exchange.getRequest().getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER);
            Map<String, Object> payload = new HashMap<>();
            payload.put("code", 429);
            payload.put("message", "too many requests");
            payload.put("traceId", traceId == null ? "" : traceId);
            payload.put("timestamp", Instant.now().toString());
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload);
        }
    }
}
