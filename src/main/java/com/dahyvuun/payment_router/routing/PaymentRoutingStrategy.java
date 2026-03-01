package com.dahyvuun.payment_router.routing;

import com.dahyvuun.payment_router.domain.PaymentRoute;
import java.util.List;

public interface PaymentRoutingStrategy {
    PaymentRoute selectRoute(List<PaymentRoute> routes);
    String getStrategyName();
}