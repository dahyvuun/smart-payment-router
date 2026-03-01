package com.dahyvuun.payment_router.routing;

import com.dahyvuun.payment_router.domain.PaymentRoute;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;

@Component
public class LowestFeeStrategy implements PaymentRoutingStrategy {

    @Override
    public PaymentRoute selectRoute(List<PaymentRoute> routes) {
        return routes.stream()
                .min(Comparator.comparing(PaymentRoute::getFeeRate))
                .orElseThrow(() -> new RuntimeException("No routes available"));
    }

    @Override
    public String getStrategyName() {
        return "LOWEST_FEE";
    }
}