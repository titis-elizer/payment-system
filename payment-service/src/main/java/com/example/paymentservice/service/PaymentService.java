package com.example.paymentservice.service;

import com.example.paymentservice.client.PaymentGatewayClient;
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

    public Payment process(String transactionId, BigDecimal amount) {
        // idempotency check (prevent double charge)
        Optional<Payment> existing = repository.findByTransactionId(transactionId);
        if (existing.isPresent()) return existing.get();

        Payment payment = new Payment();
        payment.setTransactionId(transactionId);
        payment.setAmount(amount);

        String result = gatewayClient.charge(transactionId);
        payment.setStatus(result);

        Payment saved;
        try {
            saved = repository.save(payment);
        } catch (DataIntegrityViolationException ex) {
            // Concurrent duplicate request: another transaction already inserted same txId.
            return repository.findByTransactionId(transactionId).orElseThrow(() -> ex);
        }

        if ("SUCCESS".equals(saved.getStatus())) {
            kafkaTemplate.send(
                    "payment-success",
                    new PaymentSuccessEvent(saved.getTransactionId(), saved.getAmount())
            );
        }
        return saved;
    }

    // callback idempotency
    public void handleCallback(String transactionId) {
        repository.findByTransactionId(transactionId)
                .ifPresent(payment -> {
                    if (!"SUCCESS".equals(payment.getStatus())) {
                        payment.setStatus("SUCCESS");
                        repository.save(payment);
                    }
                });
    }
}

