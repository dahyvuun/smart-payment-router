package com.dahyvuun.payment_router.routing;

import com.dahyvuun.payment_router.domain.PaymentRoute;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RoutingEngine {

    private final LowestFeeStrategy lowestFeeStrategy;
    private final HighestSuccessRateStrategy highestSuccessRateStrategy;

    public List<PaymentRoute> buildAvailableRoutes() {
        return List.of(
                PaymentRoute.builder()
                        .paymentMethod("CARD")
                        .feeRate(new BigDecimal("2.5"))
                        .successRate(new BigDecimal("95.0"))
                        .selected(false)
                        .build(),
                PaymentRoute.builder()
                        .paymentMethod("BANK_TRANSFER")
                        .feeRate(new BigDecimal("0.5"))
                        .successRate(new BigDecimal("98.0"))
                        .selected(false)
                        .build(),
                PaymentRoute.builder()
                        .paymentMethod("DIGITAL_WALLET")
                        .feeRate(new BigDecimal("1.0"))
                        .successRate(new BigDecimal("97.0"))
                        .selected(false)
                        .build()
        );
    }

    public PaymentRoute selectRoute(List<PaymentRoute> routes, String strategy) {
        PaymentRoutingStrategy routingStrategy = switch (strategy) {
            case "HIGHEST_SUCCESS_RATE" -> highestSuccessRateStrategy;
            default -> lowestFeeStrategy;
        };
        return routingStrategy.selectRoute(routes);
    }
}