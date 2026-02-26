package com.dahyvuun.payment_router.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class WalletRequest {

    @NotBlank
    private String currency;
}