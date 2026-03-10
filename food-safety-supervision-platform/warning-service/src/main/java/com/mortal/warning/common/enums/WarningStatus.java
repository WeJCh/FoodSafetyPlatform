package com.mortal.warning.common.enums;

import java.util.Locale;
import org.springframework.util.StringUtils;

/**
 * 预警状态枚举。
 */
public enum WarningStatus {
    OPEN,
    PROCESSING,
    RESOLVED,
    CLOSED;

    public static WarningStatus fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("warning status required");
        }
        try {
            return WarningStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid warning status");
        }
    }
}
