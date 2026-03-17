package com.mortal.regulation.operation.common;

public record RequestIdentity(Long userId, String userType) {

    public boolean isEnterprise() {
        return "ENTERPRISE".equalsIgnoreCase(userType);
    }

    public boolean isRegulator() {
        return "REGULATOR".equalsIgnoreCase(userType) || "ADMIN".equalsIgnoreCase(userType);
    }
}
