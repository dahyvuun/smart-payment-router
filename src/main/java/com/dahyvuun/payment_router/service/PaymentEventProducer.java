package com.dahyvuun.payment_router.service;

import com.dahyvuun.payment_router.dto.PaymentEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC = "payment-events";

    public void sendPaymentEvent(PaymentEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, event.getTransactionId().toString(), message);
            log.info("Payment event sent to Kafka: transactionId={}", event.getTransactionId());
        } catch (Exception e) {
            log.error("Failed to send payment event to Kafka", e);
        }
    }
}