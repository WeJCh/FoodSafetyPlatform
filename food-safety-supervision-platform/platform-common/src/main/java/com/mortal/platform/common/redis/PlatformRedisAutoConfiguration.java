package com.mortal.platform.common.redis;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import java.time.Duration;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@AutoConfiguration
@ConditionalOnClass(RedisConnectionFactory.class)
@EnableConfigurationProperties(PlatformRedisProperties.class)
public class PlatformRedisAutoConfiguration {

    private static final String FIXED_WINDOW_RATE_LIMIT_SCRIPT =
        "scripts/redis/fixed_window_rate_limit.lua";

    @Bean
    @ConditionalOnMissingBean
    public GenericJackson2JsonRedisSerializer platformRedisSerializer(ObjectMapper objectMapper) {
        ObjectMapper redisObjectMapper = objectMapper.copy();
        redisObjectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(redisObjectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(name = "platformRedisTemplate")
    public RedisTemplate<String, Object> platformRedisTemplate(
        RedisConnectionFactory connectionFactory,
        GenericJackson2JsonRedisSerializer serializer
    ) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisLuaScriptLoader redisLuaScriptLoader() {
        return new RedisLuaScriptLoader();
    }

    @Bean
    @ConditionalOnMissingBean
    public PlatformRedisHealthLogger platformRedisHealthLogger(PlatformRedisProperties platformRedisProperties) {
        return new PlatformRedisHealthLogger(platformRedisProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public PlatformRedisSupport platformRedisSupport(PlatformRedisProperties platformRedisProperties,
                                                     PlatformRedisHealthLogger platformRedisHealthLogger) {
        return new PlatformRedisSupport(platformRedisProperties, platformRedisHealthLogger);
    }

    @Bean
    @ConditionalOnMissingBean(name = "fixedWindowRateLimitRedisScript")
    public DefaultRedisScript<Long> fixedWindowRateLimitRedisScript(RedisLuaScriptLoader scriptLoader) {
        return scriptLoader.load(FIXED_WINDOW_RATE_LIMIT_SCRIPT, Long.class);
    }

    @Bean(destroyMethod = "shutdown")
    @Lazy
    @ConditionalOnClass(Redisson.class)
    @ConditionalOnProperty(prefix = "platform.redis.redisson", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        config.useSingleServer()
            .setAddress(buildRedisAddress(redisProperties))
            .setDatabase(redisProperties.getDatabase())
            .setUsername(redisProperties.getUsername())
            .setPassword(redisProperties.getPassword())
            .setConnectTimeout(toMillis(redisProperties.getConnectTimeout(), Duration.ofSeconds(10)))
            .setTimeout(toMillis(redisProperties.getTimeout(), Duration.ofSeconds(3)));
        return Redisson.create(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate platformStringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    private String buildRedisAddress(RedisProperties redisProperties) {
        String host = redisProperties.getHost() == null ? "127.0.0.1" : redisProperties.getHost();
        int port = redisProperties.getPort() <= 0 ? 6379 : redisProperties.getPort();
        return "redis://" + host + ":" + port;
    }

    private int toMillis(Duration duration, Duration fallback) {
        Duration resolved = duration == null ? fallback : duration;
        return (int) resolved.toMillis();
    }
}
