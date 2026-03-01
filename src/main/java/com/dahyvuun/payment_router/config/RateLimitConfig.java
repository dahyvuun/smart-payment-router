package com.dahyvuun.payment_router.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
public class RateLimitConfig {

    private final Map<String, Bucket> paymentBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> exchangeRateBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> defaultBuckets = new ConcurrentHashMap<>();

    /**
     * 결제 API 버킷: 1분에 10번
     */
    public Bucket getPaymentBucket(String identifier) {
        return paymentBuckets.computeIfAbsent(identifier, k -> {
            log.debug("Creating payment rate limit bucket for: {}", k);
            return Bucket.builder()
                .addLimit(Bandwidth.builder()
                    .capacity(10)
                    .refillIntervally(10, Duration.ofMinutes(1))
                    .build())
                .build();
        });
    }

    /**
     * 환율 API 버킷: 1분에 30번
     */
    public Bucket getExchangeRateBucket(String identifier) {
        return exchangeRateBuckets.computeIfAbsent(identifier, k -> {
            log.debug("Creating exchange rate limit bucket for: {}", k);
            return Bucket.builder()
                .addLimit(Bandwidth.builder()
                    .capacity(30)
                    .refillIntervally(30, Duration.ofMinutes(1))
                    .build())
                .build();
        });
    }

    /**
     * 기본 API 버킷: 1분에 60번
     */
    public Bucket getDefaultBucket(String identifier) {
        return defaultBuckets.computeIfAbsent(identifier, k -> {
            log.debug("Creating default rate limit bucket for: {}", k);
            return Bucket.builder()
                .addLimit(Bandwidth.builder()
                    .capacity(60)
                    .refillIntervally(60, Duration.ofMinutes(1))
                    .build())
                .build();
        });
    }
}