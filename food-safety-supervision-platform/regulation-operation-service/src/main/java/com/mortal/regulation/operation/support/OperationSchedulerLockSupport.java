package com.mortal.regulation.operation.support;

import com.mortal.platform.common.redis.PlatformRedisProperties;
import com.mortal.platform.common.redis.PlatformRedisSupport;
import com.mortal.regulation.operation.config.OperationSchedulerLockProperties;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperationSchedulerLockSupport {

    private static final String SCENE = "operation-scheduler-lock";

    private final ObjectProvider<RedissonClient> redissonClientProvider;
    private final PlatformRedisProperties platformRedisProperties;
    private final PlatformRedisSupport platformRedisSupport;
    private final OperationSchedulerLockProperties operationSchedulerLockProperties;

    public OperationSchedulerLockSupport(ObjectProvider<RedissonClient> redissonClientProvider,
                                         PlatformRedisProperties platformRedisProperties,
                                         PlatformRedisSupport platformRedisSupport,
                                         OperationSchedulerLockProperties operationSchedulerLockProperties) {
        this.redissonClientProvider = redissonClientProvider;
        this.platformRedisProperties = platformRedisProperties;
        this.platformRedisSupport = platformRedisSupport;
        this.operationSchedulerLockProperties = operationSchedulerLockProperties;
    }

    public boolean executeWithLock(String schedulerName, Runnable task) {
        return executeWithLock(schedulerName, 0L, task);
    }

    public boolean executeWithLock(String schedulerName, long leaseSeconds, Runnable task) {
        if (!StringUtils.hasText(schedulerName)) {
            throw new IllegalArgumentException("schedulerName required");
        }
        RedissonClient redissonClient = resolveRedissonClient();
        if (redissonClient == null) {
            return false;
        }
        RLock lock = redissonClient.getLock(buildKey(schedulerName));
        boolean locked = false;
        try {
            locked = lock.tryLock(
                Math.max(0L, operationSchedulerLockProperties.getWaitSeconds()),
                Math.max(1L, leaseSeconds),
                TimeUnit.SECONDS
            );
            if (!locked) {
                return false;
            }
            task.run();
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("scheduler lock interrupted");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String buildKey(String schedulerName) {
        return String.join(":",
            normalize(platformRedisProperties.getKeyPrefix(), "fsp"),
            normalize(platformRedisProperties.getEnv(), "dev"),
            "lock",
            "scheduler",
            schedulerName.trim()
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
