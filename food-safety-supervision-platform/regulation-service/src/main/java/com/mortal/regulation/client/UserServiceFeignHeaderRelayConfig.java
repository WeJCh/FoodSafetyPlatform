package com.mortal.regulation.client;

import com.mortal.regulation.filter.TraceIdFilter;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class UserServiceFeignHeaderRelayConfig {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    @Bean
    public RequestInterceptor userServiceHeaderRelayRequestInterceptor(
        @Value("${user.internal.token:user-internal-token}") String internalToken
    ) {
        return template -> {
            HttpServletRequest request = currentRequest();
            relayHeader(template, INTERNAL_TOKEN_HEADER, normalize(internalToken));
            relayHeader(template, TraceIdFilter.TRACE_ID_HEADER, firstNonBlank(
                headerValue(request, TraceIdFilter.TRACE_ID_HEADER),
                MDC.get("traceId")
            ));
            relayHeader(template, TraceIdFilter.USER_ID_HEADER, headerValue(request, TraceIdFilter.USER_ID_HEADER));
            relayHeader(template, TraceIdFilter.USERNAME_HEADER, headerValue(request, TraceIdFilter.USERNAME_HEADER));
            relayHeader(template, TraceIdFilter.USER_TYPE_HEADER, headerValue(request, TraceIdFilter.USER_TYPE_HEADER));
            relayHeader(template, TraceIdFilter.USER_ROLES_HEADER, headerValue(request, TraceIdFilter.USER_ROLES_HEADER));
        };
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        return servletAttributes.getRequest();
    }

    private String headerValue(HttpServletRequest request, String headerName) {
        if (request == null) {
            return null;
        }
        String value = request.getHeader(headerName);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        return StringUtils.hasText(second) ? second.trim() : null;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void relayHeader(feign.RequestTemplate template, String headerName, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        template.header(headerName, value);
    }
}
