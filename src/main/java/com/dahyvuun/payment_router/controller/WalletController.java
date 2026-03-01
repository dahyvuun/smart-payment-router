package com.dahyvuun.payment_router.controller;

import com.dahyvuun.payment_router.dto.WalletRequest;
import com.dahyvuun.payment_router.dto.WalletResponse;
import com.dahyvuun.payment_router.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Wallets", description = "Manage multi-currency wallets")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;

    @Operation(summary = "Create a wallet", description = "Creates a new wallet for the authenticated user in the specified currency")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Wallet created successfully",
            content = @Content(schema = @Schema(implementation = WalletResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "id": 1,
                      "currency": "USD",
                      "balance": 0.00
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Invalid currency code"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(
            @Valid @RequestBody WalletRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(walletService.createWallet(userDetails.getUsername(), request.getCurrency()));
    }

    @Operation(summary = "Get all wallets", description = "Returns all wallets belonging to the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of wallets"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<List<WalletResponse>> getWallets(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(walletService.getWalletsByEmail(userDetails.getUsername()));
    }

    @Operation(summary = "Get wallet by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Wallet found"),
        @ApiResponse(responseCode = "404", description = "Wallet not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{id}")
    public ResponseEntity<WalletResponse> getWallet(
            @Parameter(description = "Wallet ID", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(walletService.getWalletById(id));
    }

    @Operation(summary = "Delete wallet by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Wallet deleted"),
        @ApiResponse(responseCode = "404", description = "Wallet not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWallet(
            @Parameter(description = "Wallet ID", example = "1")
            @PathVariable Long id) {
        walletService.deleteWallet(id);
        return ResponseEntity.noContent().build();
    }
}