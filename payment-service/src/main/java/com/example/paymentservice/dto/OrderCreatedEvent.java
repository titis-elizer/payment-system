package com.example.paymentservice.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderCreatedEvent {
    private Long id;
    private BigDecimal amount;
    private String customerId;
}

