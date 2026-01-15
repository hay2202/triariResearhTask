package org.example.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class ThroughputMonitor {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String THROUGHPUT_KEY_PREFIX = "throughput:";

    public ThroughputMonitor(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void increment() {
        long currentSecond = Instant.now().getEpochSecond();
        String key = THROUGHPUT_KEY_PREFIX + currentSecond;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 10, TimeUnit.SECONDS); // Keep for 10 seconds
    }

    public long getCurrentThroughput() {
        // Get throughput for the last completed second to be stable, or current second?
        // Usually current second is fluctuating. Let's take the max of last few seconds or just the last second.
        // Let's take the last second for simplicity as "current" rate.
        long lastSecond = Instant.now().getEpochSecond() - 1;
        String key = THROUGHPUT_KEY_PREFIX + lastSecond;
        Object val = redisTemplate.opsForValue().get(key);
        if (val != null) {
            if (val instanceof Integer) {
                return ((Integer) val).longValue();
            } else if (val instanceof Long) {
                return (Long) val;
            }
        }
        return 0;
    }
}
