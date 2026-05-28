package com.mortal.warning.common.enums;

import java.util.Locale;
import org.springframework.util.StringUtils;

/**
 * 预警档位枚举（API 与库表存 {@code L1}/{@code L2}，展示见 {@link #displayLabel()}）。
 * <ul>
 *   <li>{@link #L1}：初发预警。如整改逾期首次上报、投诉超量等。</li>
 *   <li>{@link #L2}：升级加重。整改类可由调度从 L1 自动升级；抽检不合格、连续检查不合格等创建时即为 L2。</li>
 * </ul>
 * 合并规则：重复上报时若任一方为 L2，则保留 L2。
 */
public enum WarningLevel {
    /** 初发档位 */
    L1,
    /** 升级加重档位 */
    L2;

    public String displayLabel() {
        return switch (this) {
            case L1 -> "初发预警";
            case L2 -> "升级预警";
        };
    }

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
