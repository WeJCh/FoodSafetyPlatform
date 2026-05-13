package com.mortal.regulation.support;

import com.mortal.regulation.entity.FoodEnterprise;
import com.mortal.regulation.entity.FoodRegulator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuditOperatorNameResolver {

    public String resolveEnterpriseOperatorName(FoodEnterprise enterprise, String username) {
        String enterpriseName = enterprise == null ? null : normalize(enterprise.getEnterpriseName());
        return format(enterpriseName, username);
    }

    public String resolveRegulatorOperatorName(FoodRegulator regulator, String username) {
        String realName = regulator == null ? null : normalize(regulator.getName());
        return format(realName, username);
    }

    public String resolveSystemOperatorName() {
        return "系统";
    }

    private String format(String displayName, String username) {
        String normalizedDisplayName = normalize(displayName);
        String normalizedUsername = normalize(username);
        if (StringUtils.hasText(normalizedDisplayName) && StringUtils.hasText(normalizedUsername)) {
            return normalizedDisplayName + "（" + normalizedUsername + "）";
        }
        if (StringUtils.hasText(normalizedDisplayName)) {
            return normalizedDisplayName;
        }
        if (StringUtils.hasText(normalizedUsername)) {
            return normalizedUsername;
        }
        return resolveSystemOperatorName();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
