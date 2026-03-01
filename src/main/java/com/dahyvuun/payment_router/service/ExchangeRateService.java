package com.dahyvuun.payment_router.service;

import com.dahyvuun.payment_router.exception.ExternalApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final WebClient exchangeRateWebClient;

    @Value("${exchange-rate.api-key}")
    private String apiKey;

    // 하드코딩 fallback 환율 - API 완전 불가 시 최후 수단
    private static final Map<String, BigDecimal> FALLBACK_RATES = Map.of(
        "USD", new BigDecimal("1.0"),
        "EUR", new BigDecimal("0.92"),
        "KRW", new BigDecimal("1350.0"),
        "GBP", new BigDecimal("0.79"),
        "JPY", new BigDecimal("149.5")
    );

    /**
     * Circuit Breaker + Retry 적용
     * Retry(3번 재시도) → 실패 시 Circuit Breaker 실패 카운트 누적
     * 실패율 50% 초과 → Circuit OPEN → fallback 즉시 호출
     */
    @CircuitBreaker(name = "exchangeRateApi", fallbackMethod = "getExchangeRatesFallback")
    @Retry(name = "exchangeRateApi")
    @Cacheable(value = "exchangeRates", key = "#baseCurrency")
    public Map<String, BigDecimal> getExchangeRates(String baseCurrency) {
        log.info("Fetching exchange rates for {} from API", baseCurrency);

        try {
            Map response = exchangeRateWebClient.get()
                    .uri("/{apiKey}/latest/{base}", apiKey, baseCurrency)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            Map<String, Object> rates = (Map<String, Object>) response.get("conversion_rates");

            return rates.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            e -> new BigDecimal(e.getValue().toString())
                    ));

        } catch (WebClientException e) {
            log.error("WebClient error fetching exchange rates for {}: {}", baseCurrency, e.getMessage());
            throw new ExternalApiException("ExchangeRateAPI", "Connection failed", e);
        } catch (Exception e) {
            log.error("Unexpected error fetching exchange rates for {}: {}", baseCurrency, e.getMessage());
            throw new ExternalApiException("ExchangeRateAPI", "Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * getExchangeRates fallback - Circuit OPEN 또는 재시도 전부 실패 시 호출
     * 시그니처: 원본과 동일 + 마지막에 Throwable 추가
     */
    public Map<String, BigDecimal> getExchangeRatesFallback(String baseCurrency, Throwable ex) {
        log.warn("Circuit Breaker fallback triggered for baseCurrency={}: {}", baseCurrency, ex.getMessage());
        // fallback: 하드코딩 환율 반환 (이전 캐시가 있으면 @Cacheable이 먼저 반환하므로 여기까지 오지 않음)
        return FALLBACK_RATES;
    }

    @CircuitBreaker(name = "exchangeRateApi", fallbackMethod = "getRateFallback")
    @Retry(name = "exchangeRateApi")
    @Cacheable(value = "exchangeRates", key = "#from + '_' + #to")
    public BigDecimal getRate(String from, String to) {
        Map<String, BigDecimal> rates = getExchangeRates(from);
        BigDecimal rate = rates.get(to);
        if (rate == null) {
            throw new ExternalApiException("ExchangeRateAPI", "Rate not found for: " + from + " -> " + to, null);
        }
        return rate;
    }

    /**
     * getRate fallback
     */
    public BigDecimal getRateFallback(String from, String to, Throwable ex) {
        log.warn("Circuit Breaker fallback for rate {}/{}: {}", from, to, ex.getMessage());
        // fallback: FALLBACK_RATES 기준으로 근사값 계산
        BigDecimal fromRate = FALLBACK_RATES.getOrDefault(from, BigDecimal.ONE);
        BigDecimal toRate = FALLBACK_RATES.getOrDefault(to, BigDecimal.ONE);
        return toRate.divide(fromRate, 6, java.math.RoundingMode.HALF_UP);
    }
}