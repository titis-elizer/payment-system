package com.example.notificationservice.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentSuccessEvent {
    private String transactionId;
    private BigDecimal amount;
}

