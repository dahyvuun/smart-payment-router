package com.dahyvuun.payment_router.controller;

import com.dahyvuun.payment_router.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/{baseCurrency}")
    public ResponseEntity<Map<String, BigDecimal>> getExchangeRates(
            @PathVariable String baseCurrency) {
        return ResponseEntity.ok(exchangeRateService.getExchangeRates(baseCurrency));
    }

    @GetMapping("/{from}/{to}")
    public ResponseEntity<BigDecimal> getRate(
            @PathVariable String from,
            @PathVariable String to) {
        return ResponseEntity.ok(exchangeRateService.getRate(from, to));
    }
}