package com.dahyvuun.payment_router.routing;

import com.dahyvuun.payment_router.domain.PaymentRoute;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;

@Component
public class HighestSuccessRateStrategy implements PaymentRoutingStrategy {

    @Override
    public PaymentRoute selectRoute(List<PaymentRoute> routes) {
        return routes.stream()
                .max(Comparator.comparing(PaymentRoute::getSuccessRate))
                .orElseThrow(() -> new RuntimeException("No routes available"));
    }

    @Override
    public String getStrategyName() {
        return "HIGHEST_SUCCESS_RATE";
    }
}