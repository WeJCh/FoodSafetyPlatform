package com.mortal.user.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortal.user.entity.User;
import com.mortal.user.filter.TraceIdFilter;
import com.mortal.user.mapper.UserMapper;
import com.mortal.user.util.TokenUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class UserAuditSupport {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final ObjectMapper objectMapper;
    private final TokenUtil tokenUtil;
    private final UserMapper userMapper;

    public UserAuditSupport(ObjectMapper objectMapper,
                            TokenUtil tokenUtil,
                            UserMapper userMapper) {
        this.objectMapper = objectMapper;
        this.tokenUtil = tokenUtil;
        this.userMapper = userMapper;
    }

    public AuditActor resolveCurrentOperator() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        AuditActor headerActor = resolveHeaderActor(request);
        if (headerActor != null) {
            return headerActor;
        }
        return resolveTokenActor(request.getHeader(AUTHORIZATION_HEADER));
    }

    public AuditActor actorFromUser(User user) {
        if (user == null) {
            return null;
        }
        return new AuditActor(
            user.getId(),
            normalize(user.getUserType()),
            formatDisplayName(user.getRealName(), user.getUsername())
        );
    }

    public String writeUserSnapshot(User user, List<String> roleCodes) {
        if (user == null) {
            return "{}";
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", user.getId());
        snapshot.put("username", user.getUsername());
        snapshot.put("realName", user.getRealName());
        snapshot.put("phone", user.getPhone());
        snapshot.put("userType", user.getUserType());
        snapshot.put("status", user.getStatus());
        snapshot.put("deleted", user.getDeleted());
        snapshot.put("roles", roleCodes == null ? List.of() : roleCodes);
        return writeJson(snapshot);
    }

    public String buildTargetName(User user) {
        if (user == null) {
            return null;
        }
        return formatDisplayName(user.getRealName(), user.getUsername());
    }

    public String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public String resolveRoleDisplayName(String roleCode) {
        String normalizedRoleCode = normalize(roleCode);
        if (normalizedRoleCode == null) {
            return null;
        }
        return switch (normalizedRoleCode) {
            case "ADMIN" -> "\u7CFB\u7EDF\u7BA1\u7406\u5458";
            case "PUBLIC" -> "\u516C\u4F17\u7528\u6237";
            case "ENTERPRISE" -> "\u4F01\u4E1A\u7528\u6237";
            case "REGULATOR_ADMIN" -> "\u533A\u57DF\u7BA1\u7406\u5458";
            case "REGULATOR_ENFORCER" -> "\u6267\u6CD5\u4EBA\u5458";
            default -> normalizeText(roleCode);
        };
    }

    private String writeJson(Map<String, Object> snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize user audit snapshot", ex);
        }
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        return servletAttributes.getRequest();
    }

    private AuditActor resolveHeaderActor(HttpServletRequest request) {
        Long userId = parseLong(request.getHeader(TraceIdFilter.USER_ID_HEADER));
        String username = normalizeText(request.getHeader(TraceIdFilter.USERNAME_HEADER));
        String userType = normalize(request.getHeader(TraceIdFilter.USER_TYPE_HEADER));
        if (userId == null && !StringUtils.hasText(username) && !StringUtils.hasText(userType)) {
            return null;
        }
        if (userId != null) {
            User operator = userMapper.selectById(userId);
            if (operator != null && !Objects.equals(operator.getDeleted(), 1)) {
                return actorFromUser(operator);
            }
        }
        return new AuditActor(userId, userType, formatDisplayName(null, username));
    }

    private AuditActor resolveTokenActor(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) {
            return null;
        }
        Long userId = parseLong(claims.getSubject());
        String username = claims.get("username", String.class);
        String userType = claims.get("userType", String.class);
        if (userId != null) {
            User operator = userMapper.selectById(userId);
            if (operator != null && !Objects.equals(operator.getDeleted(), 1)) {
                return actorFromUser(operator);
            }
        }
        if (userId == null && !StringUtils.hasText(username) && !StringUtils.hasText(userType)) {
            return null;
        }
        return new AuditActor(userId, normalize(userType), formatDisplayName(null, username));
    }

    private Claims parseClaims(String token) {
        if (!StringUtils.hasText(token) || !tokenUtil.verify(token)) {
            return null;
        }
        try {
            return tokenUtil.parseToken(token);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        return StringUtils.hasText(second) ? second.trim() : null;
    }

    private String formatDisplayName(String realName, String username) {
        String normalizedRealName = normalizeText(realName);
        String normalizedUsername = normalizeText(username);
        if (StringUtils.hasText(normalizedRealName) && StringUtils.hasText(normalizedUsername)) {
            return normalizedRealName + " (" + normalizedUsername + ")";
        }
        if (StringUtils.hasText(normalizedRealName)) {
            return normalizedRealName;
        }
        if (StringUtils.hasText(normalizedUsername)) {
            return normalizedUsername;
        }
        return null;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    public record AuditActor(Long userId, String userType, String operatorName) {
    }
}
