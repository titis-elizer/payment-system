package com.example.paymentservice.api;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.service.PaymentService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService service;

    @PostMapping
    public ResponseEntity<Payment> pay(@RequestParam String txId, @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(service.process(txId, amount));
    }

    @PostMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam String txId) {
        service.handleCallback(txId);
        return ResponseEntity.ok("OK");
    }
}

