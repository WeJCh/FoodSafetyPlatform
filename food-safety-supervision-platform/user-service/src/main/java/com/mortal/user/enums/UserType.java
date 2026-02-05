package com.mortal.user.enums;

import java.util.Arrays;

public enum UserType {
    ADMIN("ADMIN"),
    REGULATOR("REGULATOR"),
    ENTERPRISE("ENTERPRISE"),
    PUBLIC("PUBLIC");

    private final String code;

    UserType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static boolean isValid(String code) {
        if (code == null) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(item -> item.code.equals(code));
    }
}
