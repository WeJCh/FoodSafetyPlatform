package com.mortal.regulation.support;

import com.mortal.platform.common.redis.PlatformRedisCacheLookup;
import com.mortal.platform.common.redis.PlatformRedisSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortal.regulation.config.RegulationCacheProperties;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RegulationPublicCacheSupport {

    private static final String LOCK_SUFFIX = ":lock";
    private static final String SCENE = "regulation-public-cache";
    private static final String NULL_SENTINEL = "__fsp:redis:null__";

    private final RedisTemplate<String, Object> redisTemplate;
    private final PlatformRedisSupport platformRedisSupport;
    private final RegulationCacheProperties regulationCacheProperties;
    private final ObjectProvider<RedissonClient> redissonClientProvider;
    private final ObjectMapper objectMapper;

    public RegulationPublicCacheSupport(RedisTemplate<String, Object> redisTemplate,
                                        PlatformRedisSupport platformRedisSupport,
                                        RegulationCacheProperties regulationCacheProperties,
                                        ObjectProvider<RedissonClient> redissonClientProvider,
                                        ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.platformRedisSupport = platformRedisSupport;
        this.regulationCacheProperties = regulationCacheProperties;
        this.redissonClientProvider = redissonClientProvider;
        this.objectMapper = objectMapper;
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
            if (NULL_SENTINEL.equals(value)) {
                return PlatformRedisCacheLookup.nullValue();
            }
            return PlatformRedisCacheLookup.hit(value);
        } catch (Exception ex) {
            PlatformRedisCacheLookup<T> legacyCached = tryLegacyLookup(key);
            if (legacyCached != null) {
                platformRedisSupport.recordRecovery(SCENE);
                return legacyCached;
            }
            deleteQuietly(key);
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
        Object cachedValue = loaded == null ? NULL_SENTINEL : loaded;
        long resolvedTtlSeconds = loaded == null ? 60L : ttlSeconds;
        try {
            redisTemplate.opsForValue().set(key, cachedValue, platformRedisSupport.jitterTtl(resolvedTtlSeconds));
            platformRedisSupport.recordRecovery(SCENE);
        } catch (Exception ex) {
            platformRedisSupport.recordFailure(SCENE, ex);
        }
        return loaded;
    }

    private void deleteQuietly(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private <T> PlatformRedisCacheLookup<T> tryLegacyLookup(String key) {
        try {
            Object rawValue = readLegacyValue(key);
            if (rawValue == null) {
                return null;
            }
            deleteQuietly(key);
            long ttlSeconds = regulationCacheProperties.getPublicData().getDetailTtlSeconds();
            redisTemplate.opsForValue().set(key, rawValue, platformRedisSupport.jitterTtl(ttlSeconds));
            return PlatformRedisCacheLookup.hit((T) rawValue);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object readLegacyValue(String key) {
        try {
            byte[] rawBytes = redisTemplate.execute((RedisConnection connection) -> {
                byte[] keyBytes = redisTemplate.getStringSerializer().serialize(key);
                return keyBytes == null ? null : connection.stringCommands().get(keyBytes);
            });
            if (rawBytes == null || rawBytes.length == 0) {
                return null;
            }
            String rawJson = new String(rawBytes, StandardCharsets.UTF_8).trim();
            if (!rawJson.startsWith("[")) {
                return null;
            }
            JsonNode root = objectMapper.readTree(rawJson);
            return decodeLegacyNode(root);
        } catch (Exception ex) {
            return null;
        }
    }

    private Object decodeLegacyNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            if (node.size() == 2 && node.get(0).isTextual()) {
                return decodeLegacyTypedValue(node.get(0).asText(), node.get(1));
            }
            List<Object> values = new ArrayList<>(node.size());
            for (JsonNode child : node) {
                values.add(decodeLegacyNode(child));
            }
            return values;
        }
        if (node.isObject()) {
            Map<String, Object> values = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                values.put(entry.getKey(), decodeLegacyNode(entry.getValue()));
            }
            return values;
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return node.asText();
    }

    private Object decodeLegacyTypedValue(String typeName, JsonNode valueNode) {
        return switch (typeName) {
            case "java.lang.Long", "long" -> valueNode.isNull() ? null : valueNode.longValue();
            case "java.lang.Integer", "int" -> valueNode.isNull() ? null : valueNode.intValue();
            case "java.lang.Double", "double" -> valueNode.isNull() ? null : valueNode.doubleValue();
            case "java.lang.Float", "float" -> valueNode.isNull() ? null : valueNode.floatValue();
            case "java.lang.Boolean", "boolean" -> valueNode.isNull() ? null : valueNode.booleanValue();
            case "java.lang.String" -> valueNode.isNull() ? null : valueNode.asText();
            default -> decodeLegacyNode(valueNode);
        };
    }
}
