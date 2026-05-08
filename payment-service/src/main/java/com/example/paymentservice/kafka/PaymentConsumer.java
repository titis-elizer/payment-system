package com.example.paymentservice.kafka;

import com.example.paymentservice.dto.OrderCreatedEvent;
import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentConsumer {
    private final PaymentService paymentService;

    @KafkaListener(topics = "order-created", groupId = "payment-service")
    public void consume(OrderCreatedEvent event) {
        paymentService.process(
                "TX-" + event.getId(),
                event.getAmount()
        );
    }
}

