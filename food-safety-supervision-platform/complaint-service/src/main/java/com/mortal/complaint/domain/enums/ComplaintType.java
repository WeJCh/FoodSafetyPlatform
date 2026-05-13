package com.mortal.complaint.domain.enums;

import java.util.Arrays;

public enum ComplaintType {
    FOOD_SAFETY("FOOD_SAFETY", "食品安全"),
    HYGIENE("HYGIENE", "卫生环境"),
    PRICE("PRICE", "价格收费"),
    FALSE_AD("FALSE_AD", "虚假宣传"),
    LICENSE("LICENSE", "资质证照"),
    SERVICE("SERVICE", "服务纠纷"),
    PACKAGING("PACKAGING", "包装配送"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String label;

    ComplaintType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static ComplaintType fromCode(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.trim().toUpperCase();
        return Arrays.stream(values())
            .filter(item -> item.code.equals(normalized))
            .findFirst()
            .orElse(null);
    }
}
