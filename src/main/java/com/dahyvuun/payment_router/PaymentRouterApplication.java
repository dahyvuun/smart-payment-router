package com.dahyvuun.payment_router;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PaymentRouterApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentRouterApplication.class, args);
    }
}