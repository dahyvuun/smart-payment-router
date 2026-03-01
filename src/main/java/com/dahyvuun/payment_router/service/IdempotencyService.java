package com.dahyvuun.payment_router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofHours(24);

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + key));
    }

    public Optional<String> get(String key) {
        String value = redisTemplate.opsForValue().get(PREFIX + key);
        return Optional.ofNullable(value);
    }

    public void save(String key, String response) {
        redisTemplate.opsForValue().set(PREFIX + key, response, TTL);
        log.info("Idempotency key saved: {}", key);
    }
}