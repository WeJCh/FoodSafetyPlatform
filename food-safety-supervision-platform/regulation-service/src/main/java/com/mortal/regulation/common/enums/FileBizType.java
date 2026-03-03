package com.mortal.regulation.common.enums;

import org.springframework.util.StringUtils;

/**
 * File upload business type used to separate object key prefixes in MinIO.
 */
public enum FileBizType {
    COMPLAINT("complaints"),
    RECTIFICATION("rectifications"),
    INSPECTION("inspections");

    private final String prefix;

    FileBizType(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }
    
    
    public static FileBizType fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("biz type required");
        }
        try {
            return FileBizType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid biz type");
        }
    }
}
