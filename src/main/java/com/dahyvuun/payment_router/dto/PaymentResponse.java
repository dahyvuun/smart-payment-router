package com.dahyvuun.payment_router.dto;

import com.dahyvuun.payment_router.domain.PaymentRoute;
import com.dahyvuun.payment_router.domain.Transaction;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class PaymentResponse {

    private Long transactionId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String selectedPaymentMethod;
    private BigDecimal feeRate;
    private BigDecimal successRate;
    private LocalDateTime createdAt;

    public PaymentResponse(Transaction transaction, PaymentRoute selectedRoute) {
        this.transactionId = transaction.getId();
        this.amount = transaction.getAmount();
        this.currency = transaction.getCurrency();
        this.status = transaction.getStatus();
        this.selectedPaymentMethod = selectedRoute.getPaymentMethod();
        this.feeRate = selectedRoute.getFeeRate();
        this.successRate = selectedRoute.getSuccessRate();
        this.createdAt = transaction.getCreatedAt();
    }
}