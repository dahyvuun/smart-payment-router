package com.dahyvuun.payment_router.service;

import com.dahyvuun.payment_router.domain.User;
import com.dahyvuun.payment_router.domain.Wallet;
import com.dahyvuun.payment_router.dto.WalletResponse;
import com.dahyvuun.payment_router.exception.ResourceNotFoundException;
import com.dahyvuun.payment_router.repository.UserRepository;
import com.dahyvuun.payment_router.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    @Transactional
    public WalletResponse createWallet(String email, String currency) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        Wallet wallet = Wallet.builder()
                .user(user)
                .currency(currency)
                .balance(BigDecimal.ZERO)
                .build();

        return new WalletResponse(walletRepository.save(wallet));
    }

    public List<WalletResponse> getWalletsByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return walletRepository.findByUserId(user.getId())
                .stream()
                .map(WalletResponse::new)
                .toList();
    }

    public WalletResponse getWalletById(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + walletId));
        return new WalletResponse(wallet);
    }

    @Transactional
    public void deleteWallet(Long walletId) {
        if (!walletRepository.existsById(walletId)) {
            throw new ResourceNotFoundException("Wallet not found: " + walletId);
        }
        walletRepository.deleteById(walletId);
    }
}