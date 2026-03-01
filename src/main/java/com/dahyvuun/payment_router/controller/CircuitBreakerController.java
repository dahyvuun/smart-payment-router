package com.dahyvuun.payment_router.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/circuit-breakers")
@RequiredArgsConstructor
public class CircuitBreakerController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * 모든 Circuit Breaker 상태 조회
     * GET /api/v1/admin/circuit-breakers
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCircuitBreakerStatus() {
        Map<String, Object> status = circuitBreakerRegistry.getAllCircuitBreakers()
            .stream()
            .collect(Collectors.toMap(
                CircuitBreaker::getName,
                this::buildMetricsMap
            ));
        return ResponseEntity.ok(status);
    }

    /**
     * 특정 Circuit Breaker 상태 조회
     * GET /api/v1/admin/circuit-breakers/{name}
     * 예: GET /api/v1/admin/circuit-breakers/exchangeRateApi
     */
    @GetMapping("/{name}")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus(@PathVariable String name) {
        return circuitBreakerRegistry.getAllCircuitBreakers()
            .stream()
            .filter(cb -> cb.getName().equals(name))
            .findFirst()
            .map(cb -> ResponseEntity.ok(buildMetricsMap(cb)))
            .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> buildMetricsMap(CircuitBreaker cb) {
        CircuitBreaker.Metrics metrics = cb.getMetrics();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("state", cb.getState().name());
        map.put("failureRate", metrics.getFailureRate());
        map.put("slowCallRate", metrics.getSlowCallRate());
        map.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
        map.put("numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls());
        map.put("numberOfNotPermittedCalls", metrics.getNumberOfNotPermittedCalls());
        map.put("numberOfSlowCalls", metrics.getNumberOfSlowCalls());
        map.put("bufferedCalls", metrics.getNumberOfBufferedCalls());
        return map;
    }
}