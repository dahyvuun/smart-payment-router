package com.dahyvuun.payment_router.service;

import com.dahyvuun.payment_router.dto.PaymentEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-events", groupId = "payment-group")
    public void consumePaymentEvent(String message) {
        try {
            PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);
            log.info("Payment event received: transactionId={}, amount={} {}, method={}, status={}",
                    event.getTransactionId(),
                    event.getAmount(),
                    event.getCurrency(),
                    event.getPaymentMethod(),
                    event.getStatus());
        } catch (Exception e) {
            log.error("Failed to process payment event", e);
        }
    }
}