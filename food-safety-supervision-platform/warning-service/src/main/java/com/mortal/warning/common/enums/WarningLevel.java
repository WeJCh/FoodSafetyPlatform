package com.mortal.warning.common.enums;

import java.util.Locale;
import org.springframework.util.StringUtils;

/**
 * 预警等级枚举。
 */
public enum WarningLevel {
    L1,
    L2;

    public static WarningLevel fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("level required");
        }
        try {
            return WarningLevel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid level");
        }
    }
}
