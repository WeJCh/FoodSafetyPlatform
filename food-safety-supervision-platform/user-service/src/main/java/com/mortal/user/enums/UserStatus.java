package com.mortal.user.enums;

public enum UserStatus {
    ENABLED(1),
    DISABLED(0);

    private final int code;

    UserStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
