package com.mortal.warning.support;

import com.mortal.platform.common.redis.PlatformRedisProperties;
import com.mortal.platform.common.redis.PlatformRedisSupport;
import com.mortal.warning.config.WarningLockProperties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WarningLockSupport {

    private static final String SCENE = "warning-lock";

    private final ObjectProvider<RedissonClient> redissonClientProvider;
    private final PlatformRedisProperties platformRedisProperties;
    private final PlatformRedisSupport platformRedisSupport;
    private final WarningLockProperties warningLockProperties;

    public WarningLockSupport(ObjectProvider<RedissonClient> redissonClientProvider,
                              PlatformRedisProperties platformRedisProperties,
                              PlatformRedisSupport platformRedisSupport,
                              WarningLockProperties warningLockProperties) {
        this.redissonClientProvider = redissonClientProvider;
        this.platformRedisProperties = platformRedisProperties;
        this.platformRedisSupport = platformRedisSupport;
        this.warningLockProperties = warningLockProperties;
    }

    public <T> T executeWithLock(String action, Long bizId, Callable<T> task) {
        if (bizId == null || bizId <= 0) {
            throw new IllegalArgumentException("lock bizId required");
        }
        RedissonClient redissonClient = resolveRedissonClient();
        if (redissonClient == null) {
            throw new IllegalStateException("distributed lock unavailable");
        }
        RLock lock = redissonClient.getLock(buildKey(action, bizId));
        boolean locked = false;
        try {
            locked = lock.tryLock(
                Math.max(0L, warningLockProperties.getWaitSeconds()),
                Math.max(1L, warningLockProperties.getLeaseSeconds()),
                TimeUnit.SECONDS
            );
            if (!locked) {
                throw new IllegalStateException("request is processing");
            }
            return task.call();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("request interrupted");
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String buildKey(String action, Long bizId) {
        return String.join(":",
            normalize(platformRedisProperties.getKeyPrefix(), "fsp"),
            normalize(platformRedisProperties.getEnv(), "dev"),
            "lock",
            normalize(action, "warning"),
            String.valueOf(bizId)
        );
    }

    private String normalize(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private RedissonClient resolveRedissonClient() {
        try {
            RedissonClient redissonClient = redissonClientProvider.getIfAvailable();
            if (redissonClient == null) {
                platformRedisSupport.recordFailure(SCENE, new IllegalStateException("Redisson client unavailable"));
                return null;
            }
            platformRedisSupport.recordRecovery(SCENE);
            return redissonClient;
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
            return null;
        }
    }
}
