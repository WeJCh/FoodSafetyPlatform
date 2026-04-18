package com.mortal.regulation.support;

import com.mortal.platform.common.redis.PlatformRedisSupport;
import com.mortal.regulation.config.RegulationCacheProperties;
import com.mortal.regulation.vo.RegionVO;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RegionCacheService {

    private static final String DOMAIN = "reg";
    private static final String REGION = "region";
    private static final String CHILDREN = "children";
    private static final String PATH = "path";
    private static final String ROOT = "root";
    private static final String LOCK_SUFFIX = ":lock";
    private static final String SCENE = "regulation-region-cache";

    private final RedisTemplate<String, Object> redisTemplate;
    private final PlatformRedisSupport platformRedisSupport;
    private final RegulationCacheProperties regulationCacheProperties;
    private final ObjectProvider<RedissonClient> redissonClientProvider;

    public RegionCacheService(RedisTemplate<String, Object> redisTemplate,
                              PlatformRedisSupport platformRedisSupport,
                              RegulationCacheProperties regulationCacheProperties,
                              ObjectProvider<RedissonClient> redissonClientProvider) {
        this.redisTemplate = redisTemplate;
        this.platformRedisSupport = platformRedisSupport;
        this.regulationCacheProperties = regulationCacheProperties;
        this.redissonClientProvider = redissonClientProvider;
    }

    public List<RegionVO> getChildren(Long parentId, Supplier<List<RegionVO>> loader) {
        String key = childrenKey(parentId);
        return getOrLoad(key, loader);
    }

    public List<RegionVO> getPath(Long regionId, Supplier<List<RegionVO>> loader) {
        if (regionId == null) {
            return List.of();
        }
        return getOrLoad(pathKey(regionId), loader);
    }

    public void evictChildren(Long parentId) {
        try {
            redisTemplate.delete(childrenKey(parentId));
            platformRedisSupport.recordRecovery(SCENE);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
        }
    }

    public void evictPath(Long regionId) {
        if (regionId == null) {
            return;
        }
        try {
            redisTemplate.delete(pathKey(regionId));
            platformRedisSupport.recordRecovery(SCENE);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
        }
    }

    private List<RegionVO> getOrLoad(String key, Supplier<List<RegionVO>> loader) {
        RegulationCacheProperties.Region config = regulationCacheProperties.getRegion();
        if (!config.isEnabled()) {
            return safeList(loader.get());
        }

        try {
            List<RegionVO> cached = readList(key);
            if (cached != null) {
                return cached;
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
                    List<RegionVO> retryCached = readList(key);
                    if (retryCached != null) {
                        return retryCached;
                    }
                    return safeList(loader.get());
                }
                List<RegionVO> retryCached = readList(key);
                if (retryCached != null) {
                    return retryCached;
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
            return safeList(loader.get());
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
            return safeList(loader.get());
        }
    }

    private List<RegionVO> loadAndCache(String key, Supplier<List<RegionVO>> loader, long ttlSeconds) {
        List<RegionVO> loaded = safeList(loader.get());
        try {
            redisTemplate.opsForValue().set(key, loaded, platformRedisSupport.jitterTtl(ttlSeconds));
            platformRedisSupport.recordRecovery(SCENE);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
        }
        return loaded;
    }

    @SuppressWarnings("unchecked")
    private List<RegionVO> readList(String key) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            platformRedisSupport.recordRecovery(SCENE);
            if (cached instanceof List<?> list) {
                return (List<RegionVO>) list;
            }
            return null;
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
            return null;
        }
    }

    private List<RegionVO> safeList(List<RegionVO> source) {
        return source == null ? List.of() : source;
    }

    private String childrenKey(Long parentId) {
        return key(DOMAIN, REGION, CHILDREN, parentId == null ? ROOT : String.valueOf(parentId));
    }

    private String pathKey(Long regionId) {
        return key(DOMAIN, REGION, PATH, String.valueOf(regionId));
    }

    private String key(String... segments) {
        return platformRedisSupport.buildKey(segments);
    }
}
