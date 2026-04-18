package com.mortal.query.support;

import com.mortal.platform.common.redis.PlatformRedisCacheLookup;
import com.mortal.platform.common.redis.PlatformRedisSupport;
import com.mortal.platform.common.redis.PlatformRedisNullValue;
import com.mortal.query.config.QueryCacheProperties;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class QueryRedisCacheSupport {

    private static final String LOCK_SUFFIX = ":lock";
    private static final String SCENE = "query-cache";

    private final RedisTemplate<String, Object> redisTemplate;
    private final PlatformRedisSupport platformRedisSupport;
    private final QueryCacheProperties queryCacheProperties;
    private final ObjectProvider<RedissonClient> redissonClientProvider;

    public QueryRedisCacheSupport(RedisTemplate<String, Object> redisTemplate,
                                  PlatformRedisSupport platformRedisSupport,
                                  QueryCacheProperties queryCacheProperties,
                                  ObjectProvider<RedissonClient> redissonClientProvider) {
        this.redisTemplate = redisTemplate;
        this.platformRedisSupport = platformRedisSupport;
        this.queryCacheProperties = queryCacheProperties;
        this.redissonClientProvider = redissonClientProvider;
    }

    public <T> T getScopeOrLoad(String key, Supplier<T> loader) {
        QueryCacheProperties.Scope config = queryCacheProperties.getScope();
        if (!config.isEnabled()) {
            return loader.get();
        }
        return getOrLoad(key, loader, config.getTtlSeconds(), config.getLockLeaseSeconds());
    }

    public <T> T getOverviewOrLoad(String key, Supplier<T> loader) {
        QueryCacheProperties.SupervisionOverview config = queryCacheProperties.getSupervisionOverview();
        if (!config.isEnabled()) {
            return loader.get();
        }
        return getOrLoad(key, loader, config.getTtlSeconds(), config.getLockLeaseSeconds());
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
