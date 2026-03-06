package com.mortal.warning.common.enums;

import java.util.Locale;
import org.springframework.util.StringUtils;

/**
 * 预警动作枚举。
 */
public enum WarningActionType {
    EVENT_UPSERT,
    ACK,
    ASSIGN,
    PROCESS,
    RESOLVE,
    CLOSE,
    LEVEL_UP,
    AUTO_LEVEL_UP;

    public static WarningActionType fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("actionType required");
        }
        try {
            return WarningActionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unsupported actionType");
        }
    }
}
