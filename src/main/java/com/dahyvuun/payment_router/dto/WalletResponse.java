package com.dahyvuun.payment_router.dto;

import com.dahyvuun.payment_router.domain.Wallet;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class WalletResponse {

    private Long id;
    private String currency;
    private BigDecimal balance;
    private LocalDateTime createdAt;

    public WalletResponse(Wallet wallet) {
        this.id = wallet.getId();
        this.currency = wallet.getCurrency();
        this.balance = wallet.getBalance();
        this.createdAt = wallet.getCreatedAt();
    }
}