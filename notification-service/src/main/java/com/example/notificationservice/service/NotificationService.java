package com.example.notificationservice.service;

import com.example.notificationservice.dto.PaymentSuccessEvent;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.events.NotificationSentEvent;
import com.example.notificationservice.repository.NotificationRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Notification handlePaymentSuccess(PaymentSuccessEvent payment) {
        LocalDateTime now = LocalDateTime.now();

        Notification n = new Notification();
        n.setTransactionId(payment.getTransactionId());
        n.setAmount(payment.getAmount());
        n.setSenderCustomerId(payment.getCustomerId());
        n.setLocalDateTime(now);
        n.setChannel("SYSTEM");
        n.setStatus("SENT");
        n.setMessage(buildMessage(payment, now));

        Notification saved = repository.save(n);

        kafkaTemplate.send(
                "notification-sent",
                new NotificationSentEvent(
                        saved.getTransactionId(),
                        saved.getAmount(),
                        saved.getSenderCustomerId(),
                        saved.getLocalDateTime(),
                        saved.getMessage(),
                        saved.getChannel()
                )
        );

        return saved;
    }

    private String buildMessage(PaymentSuccessEvent payment, LocalDateTime now) {
        return "Payment received. txId=" + payment.getTransactionId()
                + ", amount=" + payment.getAmount()
                + ", from=" + payment.getCustomerId()
                + ", at=" + now;
    }
}

