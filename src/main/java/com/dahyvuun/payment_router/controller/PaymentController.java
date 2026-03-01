package com.dahyvuun.payment_router.controller;

import com.dahyvuun.payment_router.dto.PaymentRequest;
import com.dahyvuun.payment_router.dto.PaymentResponse;
import com.dahyvuun.payment_router.service.PaymentService;
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

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Process payments with intelligent routing strategies")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(
        summary = "Process a payment",
        description = """
            Routes the payment through the optimal payment gateway based on the selected strategy:
            
            - **LOWEST_FEE** (default): Selects the gateway with the lowest transaction fee
            - **HIGHEST_SUCCESS_RATE**: Selects the gateway with the highest success rate
            
            Use the `X-Idempotency-Key` header to safely retry requests without duplicate payments.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment processed successfully",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "transactionId": 1,
                      "amount": 100.00,
                      "currency": "USD",
                      "status": "COMPLETED",
                      "selectedPaymentMethod": "BANK_TRANSFER",
                      "feeRate": 0.5,
                      "successRate": 98.0,
                      "createdAt": "2026-03-01T12:00:00"
                    }
                    """))),
        @ApiResponse(responseCode = "404", description = "Wallet not found for the given currency"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded (10 requests/minute)"),
        @ApiResponse(responseCode = "503", description = "Circuit breaker OPEN - payment gateway unavailable"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Unique key to prevent duplicate payments on retry",
                       example = "payment-2026-03-01-001")
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(
            paymentService.processPayment(userDetails.getUsername(), request, idempotencyKey));
    }
}