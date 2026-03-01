package com.dahyvuun.payment_router.service;

import com.dahyvuun.payment_router.domain.PaymentRoute;
import com.dahyvuun.payment_router.domain.Transaction;
import com.dahyvuun.payment_router.domain.Wallet;
import com.dahyvuun.payment_router.dto.PaymentEvent;
import com.dahyvuun.payment_router.dto.PaymentRequest;
import com.dahyvuun.payment_router.dto.PaymentResponse;
import com.dahyvuun.payment_router.exception.ExternalApiException;
import com.dahyvuun.payment_router.exception.ResourceNotFoundException;
import com.dahyvuun.payment_router.repository.PaymentRouteRepository;
import com.dahyvuun.payment_router.repository.TransactionRepository;
import com.dahyvuun.payment_router.repository.UserRepository;
import com.dahyvuun.payment_router.routing.RoutingEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final RoutingEngine routingEngine;
    private final TransactionRepository transactionRepository;
    private final PaymentRouteRepository paymentRouteRepository;
    private final UserRepository userRepository;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;
    private final PaymentEventProducer paymentEventProducer;

    @Transactional
    public PaymentResponse processPayment(String email, PaymentRequest request, String idempotencyKey) {

        // 1. Idempotency 체크 (기존 코드 그대로)
        if (idempotencyKey != null) {
            Optional<String> cached = idempotencyService.get(idempotencyKey);
            if (cached.isPresent()) {
                log.info("Duplicate request detected, returning cached response for key: {}", idempotencyKey);
                try {
                    return objectMapper.readValue(cached.get(), PaymentResponse.class);
                } catch (Exception e) {
                    log.error("Failed to deserialize cached response", e);
                }
            }
        }

        // 2. 유저 확인 (기존 코드 그대로)
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        // 3. 지갑 확인 (기존 코드 그대로)
        Wallet wallet = user.getWallets().stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + email));

        // 4. 라우팅 (기존 코드 그대로)
        List<PaymentRoute> availableRoutes = routingEngine.buildAvailableRoutes();
        PaymentRoute selectedRoute = routingEngine.selectRoute(availableRoutes, request.getStrategy());

        // 5. Transaction 저장 (기존 코드 그대로)
        Transaction transaction = Transaction.builder()
                .wallet(wallet)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status("COMPLETED")
                .build();
        transaction = transactionRepository.save(transaction);

        // 6. PaymentRoute 저장 - Circuit Breaker 적용
        //    실제 외부 게이트웨이 연동 시 이 메서드 내부에서 WebClient 호출
        selectedRoute = savePaymentRouteWithCircuitBreaker(transaction, selectedRoute);

        PaymentResponse response = new PaymentResponse(transaction, selectedRoute);

        // 7. Idempotency 저장 (기존 코드 그대로)
        if (idempotencyKey != null) {
            try {
                idempotencyService.save(idempotencyKey, objectMapper.writeValueAsString(response));
            } catch (Exception e) {
                log.error("Failed to cache idempotency response", e);
            }
        }

        // 8. Kafka 이벤트 발행 (기존 코드 그대로)
        PaymentEvent event = new PaymentEvent(
                transaction.getId(),
                email,
                transaction.getAmount(),
                transaction.getCurrency(),
                selectedRoute.getPaymentMethod(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
        paymentEventProducer.sendPaymentEvent(event);

        log.info("Payment processed: {} {} via {}", request.getAmount(), request.getCurrency(), selectedRoute.getPaymentMethod());
        return response;
    }

    /**
     * Circuit Breaker 적용된 결제 라우트 저장 메서드
     *
     * 현재는 DB 저장만 하지만, 추후 외부 게이트웨이 API 호출 시
     * 이 메서드 안에서 WebClient 호출을 추가하면 됨.
     *
     * Circuit Breaker 동작:
     * - CLOSED: 정상 처리
     * - OPEN: 즉시 fallback 호출 (외부 호출 없음)
     * - HALF_OPEN: 테스트 요청 후 상태 결정
     */
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "savePaymentRouteFallback")
    public PaymentRoute savePaymentRouteWithCircuitBreaker(Transaction transaction, PaymentRoute selectedRoute) {
        PaymentRoute route = PaymentRoute.builder()
                .transaction(transaction)
                .paymentMethod(selectedRoute.getPaymentMethod())
                .feeRate(selectedRoute.getFeeRate())
                .successRate(selectedRoute.getSuccessRate())
                .selected(true)
                .build();

        paymentRouteRepository.save(route);
        log.info("Payment route saved: method={}, fee={}", route.getPaymentMethod(), route.getFeeRate());
        return route;
    }

    /**
     * Circuit Breaker fallback - paymentGateway OPEN 시 호출
     * 시그니처: 원본과 동일 + 마지막에 Throwable 추가
     */
    public PaymentRoute savePaymentRouteFallback(Transaction transaction, PaymentRoute selectedRoute, Throwable ex) {
        log.error("Circuit Breaker OPEN for paymentGateway: {}", ex.getMessage());
        // fallback: 선택된 라우트를 그대로 반환 (DB 저장 없이)
        // 실제 운영에서는 실패 처리 또는 대체 게이트웨이로 전환
        return PaymentRoute.builder()
                .transaction(transaction)
                .paymentMethod(selectedRoute.getPaymentMethod())
                .feeRate(selectedRoute.getFeeRate())
                .successRate(selectedRoute.getSuccessRate())
                .selected(false) // fallback임을 표시
                .build();
    }
}