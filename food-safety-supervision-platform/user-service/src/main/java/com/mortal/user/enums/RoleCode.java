package com.mortal.user.enums;

public enum RoleCode {
    ADMIN("ADMIN"),
    REGULATOR_ADMIN("REGULATOR_ADMIN"),
    REGULATOR_ENFORCER("REGULATOR_ENFORCER"),
    ENTERPRISE("ENTERPRISE"),
    PUBLIC("PUBLIC");

    private final String code;

    RoleCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
