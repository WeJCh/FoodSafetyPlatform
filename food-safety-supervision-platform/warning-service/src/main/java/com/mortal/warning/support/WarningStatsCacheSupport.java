package com.mortal.warning.support;

import com.mortal.platform.common.redis.PlatformRedisCacheLookup;
import com.mortal.platform.common.redis.PlatformRedisSupport;
import com.mortal.platform.common.redis.PlatformRedisNullValue;
import com.mortal.warning.config.WarningCacheProperties;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WarningStatsCacheSupport {

    private static final String LOCK_SUFFIX = ":lock";
    private static final String SCENE = "warning-stats-cache";

    private final RedisTemplate<String, Object> redisTemplate;
    private final PlatformRedisSupport platformRedisSupport;
    private final WarningCacheProperties warningCacheProperties;
    private final ObjectProvider<RedissonClient> redissonClientProvider;

    public WarningStatsCacheSupport(RedisTemplate<String, Object> redisTemplate,
                                    PlatformRedisSupport platformRedisSupport,
                                    WarningCacheProperties warningCacheProperties,
                                    ObjectProvider<RedissonClient> redissonClientProvider) {
        this.redisTemplate = redisTemplate;
        this.platformRedisSupport = platformRedisSupport;
        this.warningCacheProperties = warningCacheProperties;
        this.redissonClientProvider = redissonClientProvider;
    }

    public <T> T getOrLoad(String key, Supplier<T> loader) {
        WarningCacheProperties.Stats config = warningCacheProperties.getStats();
        if (!config.isEnabled()) {
            return loader.get();
        }
        try {
            PlatformRedisCacheLookup<T> cached = lookup(key);
            if (cached.isHit()) {
                return cached.getValue();
            }
            RedissonClient redissonClient = redissonClientProvider.getIfAvailable();
            if (redissonClient == null) {
                return loadAndCache(key, loader, config.getTtlSeconds());
            }
            RLock lock = redissonClient.getLock(key + LOCK_SUFFIX);
            boolean locked = false;
            try {
                locked = lock.tryLock(1, config.getLockLeaseSeconds(), TimeUnit.SECONDS);
                if (!locked) {
                    PlatformRedisCacheLookup<T> retryCached = lookup(key);
                    return retryCached.isHit() ? retryCached.getValue() : loader.get();
                }
                PlatformRedisCacheLookup<T> retryCached = lookup(key);
                if (retryCached.isHit()) {
                    return retryCached.getValue();
                }
                return loadAndCache(key, loader, config.getTtlSeconds());
            } finally {
                if (locked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            platformRedisSupport.recordFailure(SCENE, ex);
            return loader.get();
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
            return loader.get();
        }
    }

    public long currentVersion() {
        try {
            Object value = redisTemplate.opsForValue().get(versionKey());
            platformRedisSupport.recordRecovery(SCENE);
            if (value instanceof Number number) {
                return Math.max(1L, number.longValue());
            }
            if (value instanceof String text && StringUtils.hasText(text)) {
                try {
                    return Math.max(1L, Long.parseLong(text));
                } catch (NumberFormatException ignored) {
                    return 1L;
                }
            }
            return 1L;
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
            return 1L;
        }
    }

    public void bumpVersion() {
        try {
            redisTemplate.opsForValue().increment(versionKey());
            platformRedisSupport.recordRecovery(SCENE);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
        }
    }

    public String buildCacheKey(String category, String fingerprint) {
        return buildKey("warning", "stats", category, "v" + currentVersion(), fingerprint);
    }

    @SuppressWarnings("unchecked")
    public <T> T read(String key) {
        PlatformRedisCacheLookup<T> cached = lookup(key);
        return cached.getValue();
    }

    @SuppressWarnings("unchecked")
    private <T> PlatformRedisCacheLookup<T> lookup(String key) {
        try {
            T value = (T) redisTemplate.opsForValue().get(key);
            platformRedisSupport.recordRecovery(SCENE);
            if (value == null) {
                return PlatformRedisCacheLookup.miss();
            }
            if (value instanceof PlatformRedisNullValue) {
                return PlatformRedisCacheLookup.nullValue();
            }
            return PlatformRedisCacheLookup.hit(value);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
            return PlatformRedisCacheLookup.miss();
        }
    }

    private String versionKey() {
        return buildKey("warning", "stats", "version");
    }

    private <T> T loadAndCache(String key, Supplier<T> loader, long ttlSeconds) {
        T loaded = loader.get();
        Object cachedValue = loaded == null ? PlatformRedisNullValue.INSTANCE : loaded;
        long resolvedTtlSeconds = loaded == null ? 60L : ttlSeconds;
        try {
            redisTemplate.opsForValue().set(key, cachedValue, platformRedisSupport.jitterTtl(resolvedTtlSeconds));
            platformRedisSupport.recordRecovery(SCENE);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
        }
        return loaded;
    }

    private String buildKey(String... segments) {
        return platformRedisSupport.buildKey(segments);
    }
}
