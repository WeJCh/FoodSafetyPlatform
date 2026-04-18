package com.mortal.regulation.support;

import com.mortal.platform.common.redis.PlatformRedisCacheLookup;
import com.mortal.platform.common.redis.PlatformRedisSupport;
import com.mortal.platform.common.redis.PlatformRedisNullValue;
import com.mortal.regulation.config.RegulationCacheProperties;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RegulationMasterCacheSupport {

    private static final String LOCK_SUFFIX = ":lock";
    private static final String SCENE = "regulation-master-cache";

    private final RedisTemplate<String, Object> redisTemplate;
    private final PlatformRedisSupport platformRedisSupport;
    private final RegulationCacheProperties regulationCacheProperties;
    private final ObjectProvider<RedissonClient> redissonClientProvider;

    public RegulationMasterCacheSupport(RedisTemplate<String, Object> redisTemplate,
                                        PlatformRedisSupport platformRedisSupport,
                                        RegulationCacheProperties regulationCacheProperties,
                                        ObjectProvider<RedissonClient> redissonClientProvider) {
        this.redisTemplate = redisTemplate;
        this.platformRedisSupport = platformRedisSupport;
        this.regulationCacheProperties = regulationCacheProperties;
        this.redissonClientProvider = redissonClientProvider;
    }

    public <T> T getOrLoad(String key, Supplier<T> loader) {
        RegulationCacheProperties.MasterData config = regulationCacheProperties.getMasterData();
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

    public void write(String key, Object value) {
        RegulationCacheProperties.MasterData config = regulationCacheProperties.getMasterData();
        try {
            Object cachedValue = value == null ? PlatformRedisNullValue.INSTANCE : value;
            long ttlSeconds = value == null ? 60L : config.getTtlSeconds();
            redisTemplate.opsForValue().set(key, cachedValue, platformRedisSupport.jitterTtl(ttlSeconds));
            platformRedisSupport.recordRecovery(SCENE);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            platformRedisSupport.recordRecovery(SCENE);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
        }
    }

    public Long increment(String key) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            platformRedisSupport.recordRecovery(SCENE);
            return value;
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
            return null;
        }
    }

    public long getLong(String key, long fallback) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            platformRedisSupport.recordRecovery(SCENE);
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String text && org.springframework.util.StringUtils.hasText(text)) {
                try {
                    return Long.parseLong(text);
                } catch (NumberFormatException ignored) {
                    return fallback;
                }
            }
            return fallback;
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
            return fallback;
        }
    }

    public String buildKey(String... segments) {
        return platformRedisSupport.buildKey(segments);
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
}
