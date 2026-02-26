package com.dahyvuun.payment_router.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "payment_routes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class PaymentRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "fee_rate", nullable = false)
    private BigDecimal feeRate;

    @Column(name = "success_rate", nullable = false)
    private BigDecimal successRate;

    @Column(nullable = false)
    private Boolean selected;
}