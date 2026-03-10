package com.mortal.warning.common.enums;

import java.util.Locale;
import org.springframework.util.StringUtils;

/**
 * 预警动作枚举。
 */
public enum WarningActionType {
    EVENT_UPSERT,
    ASSIGN,
    PROCESS,
    RESOLVE,
    AUTO_LEVEL_UP,
    AUTO_ARCHIVE;

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
