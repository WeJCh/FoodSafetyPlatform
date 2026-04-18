package com.mortal.platform.common.redis;

public final class PlatformRedisCacheLookup<T> {

    private static final PlatformRedisCacheLookup<?> MISS = new PlatformRedisCacheLookup<>(false, false, null);
    private static final PlatformRedisCacheLookup<?> NULL_VALUE = new PlatformRedisCacheLookup<>(true, true, null);

    private final boolean hit;
    private final boolean nullValue;
    private final T value;

    private PlatformRedisCacheLookup(boolean hit, boolean nullValue, T value) {
        this.hit = hit;
        this.nullValue = nullValue;
        this.value = value;
    }

    public static <T> PlatformRedisCacheLookup<T> miss() {
        @SuppressWarnings("unchecked")
        PlatformRedisCacheLookup<T> result = (PlatformRedisCacheLookup<T>) MISS;
        return result;
    }

    public static <T> PlatformRedisCacheLookup<T> nullValue() {
        @SuppressWarnings("unchecked")
        PlatformRedisCacheLookup<T> result = (PlatformRedisCacheLookup<T>) NULL_VALUE;
        return result;
    }

    public static <T> PlatformRedisCacheLookup<T> hit(T value) {
        return new PlatformRedisCacheLookup<>(true, false, value);
    }

    public boolean isHit() {
        return hit;
    }

    public boolean isNullValue() {
        return nullValue;
    }

    public T getValue() {
        return value;
    }
}
