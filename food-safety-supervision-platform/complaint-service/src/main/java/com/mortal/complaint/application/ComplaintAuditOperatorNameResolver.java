package com.mortal.complaint.application;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ComplaintAuditOperatorNameResolver {

    private static final String SYSTEM_OPERATOR_NAME = "系统";
    private static final String PUBLIC_OPERATOR_NAME = "公众用户";

    public String resolvePublicOperatorName() {
        return PUBLIC_OPERATOR_NAME;
    }

    public String resolveRegulatorOperatorName(String realName, String username) {
        String resolvedName = trimToNull(realName);
        String resolvedUsername = trimToNull(username);
        if (resolvedName != null && resolvedUsername != null) {
            return resolvedName + "（" + resolvedUsername + "）";
        }
        if (resolvedName != null) {
            return resolvedName;
        }
        if (resolvedUsername != null) {
            return resolvedUsername;
        }
        return SYSTEM_OPERATOR_NAME;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
