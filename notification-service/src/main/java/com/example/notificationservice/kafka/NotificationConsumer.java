package com.example.notificationservice.kafka;

import com.example.notificationservice.dto.PaymentSuccessEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

    @KafkaListener(topics = "payment-success", groupId = "notification-service")
    public void notify(PaymentSuccessEvent payment) {
        System.out.println("Send notification for payment: " + payment.getTransactionId());
    }
}

