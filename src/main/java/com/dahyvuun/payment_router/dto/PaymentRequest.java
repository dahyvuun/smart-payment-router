package com.dahyvuun.payment_router.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
public class PaymentRequest {

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String currency;

    private String strategy = "LOWEST_FEE";
}