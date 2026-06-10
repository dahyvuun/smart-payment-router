package com.dahyvuun.payment_router.consumer;

import com.dahyvuun.payment_router.dto.PaymentEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentNotificationConsumer {

	private final ObjectMapper objectMapper;

	@KafkaListener(
			topics = "payment-events",
			groupId = "payment-notification-group"
	)
	public void consume(String message) {
		try {
			PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);
			processNotification(event);
		} catch (Exception e) {
			log.error("Failed to process payment event: {}", message, e);
		}
	}

	private void processNotification(PaymentEvent event) {
		// 실제 운영: 이메일/SMS/푸시 알림 서비스 호출
		// 현재: 알림 발송 시뮬레이션
		log.info("[NOTIFICATION] Payment completed - transactionId={}, user={}, amount={} {}, method={}",
				event.getTransactionId(),
				event.getEmail(),
				event.getAmount(),
				event.getCurrency(),
				event.getPaymentMethod()
		);

		if ("COMPLETED".equals(event.getStatus())) {
			log.info("[NOTIFICATION] Sending success email to {}", event.getEmail());
		} else {
			log.warn("[NOTIFICATION] Payment failed for user={}, transactionId={}",
					event.getEmail(), event.getTransactionId());
		}
	}
}