package com.telemed.common.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Component
public class RedisDistributedLock {

    private static final String LOCK_PREFIX = "lock:schedule:";

    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else " +
                    "return 0 " +
                    "end";

    private final RedisTemplate<String, Object> redisTemplate;

    private final ThreadLocal<String> lockValueHolder = new ThreadLocal<>();

    public RedisDistributedLock(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryLock(String key, int expireSeconds) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = Thread.currentThread().getId() + ":" + System.nanoTime();
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, expireSeconds, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(result)) {
            lockValueHolder.set(lockValue);
            return true;
        }
        return false;
    }

    public void unlock(String key) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = lockValueHolder.get();
        if (lockValue == null) {
            return;
        }
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
            redisTemplate.execute(script, Collections.singletonList(lockKey), lockValue);
        } finally {
            lockValueHolder.remove();
        }
    }
}
