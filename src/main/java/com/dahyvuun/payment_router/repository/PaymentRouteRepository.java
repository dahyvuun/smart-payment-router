package com.dahyvuun.payment_router.repository;

import com.dahyvuun.payment_router.domain.PaymentRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRouteRepository extends JpaRepository<PaymentRoute, Long> {
    List<PaymentRoute> findByTransactionId(Long transactionId);
}