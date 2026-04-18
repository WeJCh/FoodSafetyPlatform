package com.mortal.platform.common.redis;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public class RedisLuaScriptLoader {

    public <T> DefaultRedisScript<T> load(String classpath, Class<T> resultType) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(classpath));
        script.setResultType(resultType);
        return script;
    }
}
