package com.mortal.complaint.support;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 请求身份解析器
 */
@Component
public class RequestIdentityResolver {

    public RequestIdentity resolve(String userIdValue, String userType, String userRolesValue) {
        Long userId = parseUserId(userIdValue);
        if (userId == null) {
            throw new IllegalArgumentException("unauthorized");
        }
        return new RequestIdentity(userId, normalize(userType), parseRoles(userRolesValue));
    }

    private Long parseUserId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private Set<String> parseRoles(String value) {
        if (!StringUtils.hasText(value)) {
            return Set.of();
        }
        Set<String> roles = new LinkedHashSet<>();
        Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .map(String::toUpperCase)
            .forEach(roles::add);
        return roles;
    }

    public record RequestIdentity(Long userId, String userType, Set<String> roles) {

        public boolean isPublicUser() {
            return "PUBLIC".equals(userType) || roles.contains("PUBLIC");
        }

        public boolean isRegulator() {
            return "REGULATOR".equals(userType)
                || "ADMIN".equals(userType)
                || roles.contains("REGULATOR_ADMIN")
                || roles.contains("REGULATOR_ENFORCER")
                || roles.contains("ADMIN");
        }
    }
}
