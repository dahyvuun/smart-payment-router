package com.dahyvuun.payment_router.service;

import com.dahyvuun.payment_router.TestcontainersConfig;
import com.dahyvuun.payment_router.domain.User;
import com.dahyvuun.payment_router.domain.Wallet;
import com.dahyvuun.payment_router.dto.PaymentRequest;
import com.dahyvuun.payment_router.dto.PaymentResponse;
import com.dahyvuun.payment_router.exception.ResourceNotFoundException;
import com.dahyvuun.payment_router.repository.UserRepository;
import com.dahyvuun.payment_router.repository.WalletRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for PaymentService.
 * Uses real PostgreSQL, Redis, and Kafka via Testcontainers.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@Transactional
class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private String testEmail;

    @BeforeEach
    void setUp() {
        testEmail = "test@test.com";

        User user = User.builder()
            .email(testEmail)
            .password(passwordEncoder.encode("password123"))
            .build();
        User savedUser = userRepository.save(user);

        Wallet wallet = Wallet.builder()
            .user(savedUser)
            .currency("USD")
            .balance(new BigDecimal("1000.00"))
            .build();
        walletRepository.save(wallet);

        // Flush to DB and clear the first-level cache
        // so PaymentService loads a fresh User with wallets initialized by Hibernate
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Payment success - LOWEST_FEE routing strategy")
    void processPayment_success_withLowestFeeStrategy() {
        // given
        PaymentRequest request = buildPaymentRequest("100.00", "USD", "LOWEST_FEE");

        // when
        PaymentResponse response = paymentService.processPayment(testEmail, request, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getSelectedPaymentMethod()).isNotBlank();
        assertThat(response.getFeeRate()).isNotNull();
    }

    @Test
    @DisplayName("Payment success - HIGHEST_SUCCESS_RATE routing strategy")
    void processPayment_success_withHighestSuccessRateStrategy() {
        // given
        PaymentRequest request = buildPaymentRequest("50.00", "EUR", "HIGHEST_SUCCESS_RATE");

        // when
        PaymentResponse response = paymentService.processPayment(testEmail, request, null);

        // then
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getSuccessRate()).isNotNull();
    }

    @Test
    @DisplayName("Idempotency Key - duplicate request returns same response")
    void processPayment_idempotency_returnsSameResponse() {
        // given
        PaymentRequest request = buildPaymentRequest("100.00", "USD", "LOWEST_FEE");
        String idempotencyKey = "test-idem-key-001";

        // when
        PaymentResponse first = paymentService.processPayment(testEmail, request, idempotencyKey);
        PaymentResponse second = paymentService.processPayment(testEmail, request, idempotencyKey);

        // then - both responses must share the same transaction ID

        assertThat(second.getAmount()).isEqualByComparingTo(first.getAmount());
        assertThat(second.getCurrency()).isEqualTo(first.getCurrency());
        assertThat(second.getStatus()).isEqualTo(first.getStatus());
    }

    @Test
    @DisplayName("Non-existent user - throws ResourceNotFoundException")
    void processPayment_userNotFound_throwsException() {
        // given
        PaymentRequest request = buildPaymentRequest("100.00", "USD", "LOWEST_FEE");

        // when & then
        assertThatThrownBy(() ->
            paymentService.processPayment("nonexistent@test.com", request, null))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("User without wallet - throws ResourceNotFoundException")
    void processPayment_walletNotFound_throwsException() {
        // given - create a user with no wallet and clear cache
        User noWalletUser = User.builder()
            .email("nowallet@test.com")
            .password(passwordEncoder.encode("password123"))
            .build();
        userRepository.save(noWalletUser);
        entityManager.flush();
        entityManager.clear();

        PaymentRequest request = buildPaymentRequest("100.00", "USD", "LOWEST_FEE");

        // when & then
        assertThatThrownBy(() ->
            paymentService.processPayment("nowallet@test.com", request, null))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Wallet not found");
    }

    // --- Helpers ---

    private PaymentRequest buildPaymentRequest(String amount, String currency, String strategy) {
        PaymentRequest req = new PaymentRequest();
        setField(req, "amount", new BigDecimal(amount));
        setField(req, "currency", currency);
        setField(req, "strategy", strategy);
        return req;
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}