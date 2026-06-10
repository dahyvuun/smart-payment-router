package com.dahyvuun.payment_router.routing;

import com.dahyvuun.payment_router.domain.PaymentRoute;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WeightedRoundRobinStrategy implements PaymentRoutingStrategy {

	// 게이트웨이별 가중치 (높을수록 더 많이 선택됨)
	private static final Map<String, Integer> WEIGHTS = Map.of(
			"BANK_TRANSFER", 5,
			"DIGITAL_WALLET", 3,
			"CARD", 2
	);

	private final AtomicInteger counter = new AtomicInteger(0);

	@Override
	public PaymentRoute selectRoute(List<PaymentRoute> routes) {
		// 가중치 기반 확장 리스트 생성 (예: BANK_TRANSFER 5개, DIGITAL_WALLET 3개, CARD 2개)
		List<PaymentRoute> weightedRoutes = routes.stream()
				.flatMap(route -> {
					int weight = WEIGHTS.getOrDefault(route.getPaymentMethod(), 1);
					return java.util.Collections.nCopies(weight, route).stream();
				})
				.toList();

		if (weightedRoutes.isEmpty()) {
			throw new RuntimeException("No routes available");
		}

		int index = counter.getAndIncrement() % weightedRoutes.size();
		return weightedRoutes.get(index);
	}

	@Override
	public String getStrategyName() {
		return "WEIGHTED_ROUND_ROBIN";
	}
}