package com.mortal.regulation.operation.support;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OperationAuditOperatorNameResolver {

    private static final String SYSTEM_OPERATOR_NAME = "系统";

    public String resolveRegulatorOperatorName(String realName, String username) {
        String normalizedRealName = normalize(realName);
        String normalizedUsername = normalize(username);
        if (normalizedRealName != null && normalizedUsername != null) {
            return normalizedRealName + "（" + normalizedUsername + "）";
        }
        if (normalizedRealName != null) {
            return normalizedRealName;
        }
        if (normalizedUsername != null) {
            return normalizedUsername;
        }
        return SYSTEM_OPERATOR_NAME;
    }

    public String resolveEnterpriseOperatorName(String enterpriseName) {
        String normalizedEnterpriseName = normalize(enterpriseName);
        return normalizedEnterpriseName != null ? normalizedEnterpriseName : SYSTEM_OPERATOR_NAME;
    }

    public String resolveSystemOperatorName() {
        return SYSTEM_OPERATOR_NAME;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
