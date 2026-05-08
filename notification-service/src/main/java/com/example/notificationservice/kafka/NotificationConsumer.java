package com.example.notificationservice.kafka;

import com.example.notificationservice.dto.PaymentSuccessEvent;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {
    private final NotificationService service;

    @KafkaListener(topics = "payment-success", groupId = "notification-service")
    public void notify(PaymentSuccessEvent payment) {
        service.handlePaymentSuccess(payment);
    }
}

