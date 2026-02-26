package com.dahyvuun.payment_router.controller;

import com.dahyvuun.payment_router.dto.WalletRequest;
import com.dahyvuun.payment_router.dto.WalletResponse;
import com.dahyvuun.payment_router.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(
            @Valid @RequestBody WalletRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        WalletResponse response = walletService.createWallet(userDetails.getUsername(), request.getCurrency());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<WalletResponse>> getWallets(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<WalletResponse> wallets = walletService.getWalletsByEmail(userDetails.getUsername());
        return ResponseEntity.ok(wallets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Long id) {
        return ResponseEntity.ok(walletService.getWalletById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWallet(@PathVariable Long id) {
        walletService.deleteWallet(id);
        return ResponseEntity.noContent().build();
    }
}