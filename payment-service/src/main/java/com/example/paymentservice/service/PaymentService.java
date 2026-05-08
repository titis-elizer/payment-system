package com.example.paymentservice.service;

import com.example.paymentservice.client.PaymentGatewayClient;
import com.example.paymentservice.dto.OrderCreatedEvent;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.events.PaymentSuccessEvent;
import com.example.paymentservice.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {
    private final PaymentRepository repository;
    private final PaymentGatewayClient gatewayClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Payment createPendingFromOrder(OrderCreatedEvent event) {
        String transactionId = "TX-" + event.getId();
        Optional<Payment> existing = repository.findByTransactionId(transactionId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Payment payment = new Payment();
        payment.setTransactionId(transactionId);
        payment.setOrderId(event.getId());
        payment.setCustomerId(event.getCustomerId());
        payment.setAmount(event.getAmount());
        payment.setStatus("PENDING");

        try {
            return repository.save(payment);
        } catch (DataIntegrityViolationException ex) {
            return repository.findByTransactionId(transactionId).orElseThrow(() -> ex);
        }
    }

    public Payment process(String transactionId, BigDecimal amount) {
        Optional<Payment> existing = repository.findByTransactionId(transactionId);
        Payment payment;
        if (existing.isPresent()) {
            payment = existing.get();
            if ("SUCCESS".equals(payment.getStatus())) {
                return payment;
            }
            if (payment.getAmount() == null) {
                payment.setAmount(amount);
            }
        } else {
            payment = new Payment();
            payment.setTransactionId(transactionId);
            payment.setAmount(amount);
            payment.setStatus("PENDING");
        }

        String result = gatewayClient.charge(transactionId);
        payment.setStatus(result);
        Payment saved;
        try {
            saved = repository.save(payment);
        } catch (DataIntegrityViolationException ex) {
            return repository.findByTransactionId(transactionId).orElseThrow(() -> ex);
        }

        if ("SUCCESS".equals(saved.getStatus())) {
            kafkaTemplate.send(
                    "payment-success",
                    new PaymentSuccessEvent(saved.getTransactionId(), saved.getAmount(), saved.getCustomerId())
            );
        }
        return saved;
    }

    public Payment confirm(String transactionId) {
        Payment payment = repository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown transactionId: " + transactionId));
        return process(transactionId, payment.getAmount());
    }

    // callback idempotency
    public void handleCallback(String transactionId) {
        repository.findByTransactionId(transactionId)
                .ifPresent(payment -> {
                    if (!"SUCCESS".equals(payment.getStatus())) {
                        payment.setStatus("SUCCESS");
                        Payment updated = repository.save(payment);
                        kafkaTemplate.send(
                                "payment-success",
                                new PaymentSuccessEvent(updated.getTransactionId(), updated.getAmount(), updated.getCustomerId())
                        );
                    }
                });
    }
}

