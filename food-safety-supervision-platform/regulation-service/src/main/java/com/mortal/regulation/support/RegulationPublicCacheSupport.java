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
public class RegulationPublicCacheSupport {

    private static final String LOCK_SUFFIX = ":lock";
    private static final String SCENE = "regulation-public-cache";

    private final RedisTemplate<String, Object> redisTemplate;
    private final PlatformRedisSupport platformRedisSupport;
    private final RegulationCacheProperties regulationCacheProperties;
    private final ObjectProvider<RedissonClient> redissonClientProvider;

    public RegulationPublicCacheSupport(RedisTemplate<String, Object> redisTemplate,
                                        PlatformRedisSupport platformRedisSupport,
                                        RegulationCacheProperties regulationCacheProperties,
                                        ObjectProvider<RedissonClient> redissonClientProvider) {
        this.redisTemplate = redisTemplate;
        this.platformRedisSupport = platformRedisSupport;
        this.regulationCacheProperties = regulationCacheProperties;
        this.redissonClientProvider = redissonClientProvider;
    }

    public <T> T getOrLoadList(String key, Supplier<T> loader) {
        RegulationCacheProperties.PublicData config = regulationCacheProperties.getPublicData();
        if (!config.isEnabled()) {
            return loader.get();
        }
        return getOrLoad(key, loader, config.getListTtlSeconds(), config.getLockLeaseSeconds());
    }

    public <T> T getOrLoadDetail(String key, Supplier<T> loader) {
        RegulationCacheProperties.PublicData config = regulationCacheProperties.getPublicData();
        if (!config.isEnabled()) {
            return loader.get();
        }
        return getOrLoad(key, loader, config.getDetailTtlSeconds(), config.getLockLeaseSeconds());
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            platformRedisSupport.recordRecovery(SCENE);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
        }
    }

    public void bumpVersion(String key) {
        try {
            redisTemplate.opsForValue().increment(key);
            platformRedisSupport.recordRecovery(SCENE);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
        }
    }

    public long getVersion(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            platformRedisSupport.recordRecovery(SCENE);
            if (value instanceof Number number) {
                return Math.max(1L, number.longValue());
            }
            if (value instanceof String text && org.springframework.util.StringUtils.hasText(text)) {
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

    public String buildKey(String... segments) {
        return platformRedisSupport.buildKey(segments);
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

    private <T> T getOrLoad(String key, Supplier<T> loader, long ttlSeconds, long lockLeaseSeconds) {
        try {
            PlatformRedisCacheLookup<T> cached = lookup(key);
            if (cached.isHit()) {
                return cached.getValue();
            }
            RedissonClient redissonClient = redissonClientProvider.getIfAvailable();
            if (redissonClient == null) {
                return loadAndCache(key, loader, ttlSeconds);
            }
            RLock lock = redissonClient.getLock(key + LOCK_SUFFIX);
            boolean locked = false;
            try {
                locked = lock.tryLock(1, lockLeaseSeconds, TimeUnit.SECONDS);
                if (!locked) {
                    PlatformRedisCacheLookup<T> retryCached = lookup(key);
                    return retryCached.isHit() ? retryCached.getValue() : loader.get();
                }
                PlatformRedisCacheLookup<T> retryCached = lookup(key);
                if (retryCached.isHit()) {
                    return retryCached.getValue();
                }
                return loadAndCache(key, loader, ttlSeconds);
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
