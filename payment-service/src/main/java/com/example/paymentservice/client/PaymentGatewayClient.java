package com.example.paymentservice.client;

import io.github.resilience4j.retry.annotation.Retry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

@Service
@Slf4j
public class PaymentGatewayClient {
    private final Map<String, AtomicInteger> attemptsByTx = new ConcurrentHashMap<>();

    @Value("${payment.gateway.timeout-simulation.enabled:true}")
    private boolean timeoutSimulationEnabled;

    @Value("${payment.gateway.timeout-simulation.fail-attempts:2}")
    private int failAttempts;

    @Retry(name = "paymentGateway")
    public String charge(String transactionId) {
        if (timeoutSimulationEnabled) {
            int currentAttempt = attemptsByTx
                    .computeIfAbsent(transactionId, key -> new AtomicInteger(0))
                    .incrementAndGet();

            if (currentAttempt <= failAttempts) {
                log.warn("Simulated gateway timeout txId={} attempt={}", transactionId, currentAttempt);
                throw new ResourceAccessException(
                        "Simulated network timeout for txId=" + transactionId + " attempt=" + currentAttempt
                );
            }
        }

        log.info("Gateway charge success txId={}", transactionId);
        return "SUCCESS";
    }
}

