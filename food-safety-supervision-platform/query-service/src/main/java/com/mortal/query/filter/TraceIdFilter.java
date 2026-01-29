package com.mortal.query.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        // 关键注释：写入 MDC，确保日志带 traceId，实现全链路日志追踪。
        MDC.put("traceId", traceId);
        // 关键注释：从网关透传用户信息，写入 MDC，便于日志排查权限问题。
        putIfPresent("userId", request.getHeader(USER_ID_HEADER));
        putIfPresent("roles", request.getHeader(USER_ROLES_HEADER));
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
            MDC.remove("userId");
            MDC.remove("roles");
        }
    }

    private void putIfPresent(String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        MDC.put(key, value);
    }
}
