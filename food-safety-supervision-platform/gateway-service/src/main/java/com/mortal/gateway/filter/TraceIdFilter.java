package com.mortal.gateway.filter;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String METHOD_HEADER = "X-Method";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            // 关键注释：使用更短的 traceId，便于日志人工排查。
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
        // 关键注释：在网关生成/透传 traceId，并写入请求与响应头，供全链路追踪。
        ServerWebExchange mutated = exchange.mutate()
            .request(exchange.getRequest().mutate()
                .header(TRACE_ID_HEADER, traceId)
                // 关键注释：透传请求方法，供 Sentinel 按“查询/写入”区分限流。
                .header(METHOD_HEADER, exchange.getRequest().getMethod().name())
                .build())
            .response(exchange.getResponse())
            .build();
        mutated.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);
        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        // 提前执行，确保后续过滤器可拿到 traceId
        return -200;
    }
}
