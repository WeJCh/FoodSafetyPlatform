package com.mortal.platform.common.redis;

import java.io.Serializable;

public final class PlatformRedisNullValue implements Serializable {

    public static final PlatformRedisNullValue INSTANCE = new PlatformRedisNullValue();

    private PlatformRedisNullValue() {
    }
}
