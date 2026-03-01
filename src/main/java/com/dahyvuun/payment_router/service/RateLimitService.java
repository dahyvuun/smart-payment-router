package com.dahyvuun.payment_router.service;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import com.dahyvuun.payment_router.config.RateLimitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimitConfig rateLimitConfig;

    public enum LimitType {
        PAYMENT,
        EXCHANGE_RATE,
        DEFAULT
    }

    public record RateLimitResult(
        boolean allowed,
        long remainingTokens,
        long nanosToWaitForRefill
    ) {
        public long waitSeconds() {
            return nanosToWaitForRefill / 1_000_000_000;
        }
    }

    public RateLimitResult checkRateLimit(String identifier, LimitType type) {
        Bucket bucket = getBucket(identifier, type);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            log.debug("Rate limit allowed: type={}, identifier={}, remaining={}",
                type, identifier, probe.getRemainingTokens());
        } else {
            log.warn("Rate limit exceeded: type={}, identifier={}, waitSeconds={}",
                type, identifier, probe.getNanosToWaitForRefill() / 1_000_000_000);
        }

        return new RateLimitResult(
            probe.isConsumed(),
            probe.getRemainingTokens(),
            probe.getNanosToWaitForRefill()
        );
    }

    private Bucket getBucket(String identifier, LimitType type) {
        return switch (type) {
            case PAYMENT -> rateLimitConfig.getPaymentBucket(identifier);
            case EXCHANGE_RATE -> rateLimitConfig.getExchangeRateBucket(identifier);
            case DEFAULT -> rateLimitConfig.getDefaultBucket(identifier);
        };
    }
}