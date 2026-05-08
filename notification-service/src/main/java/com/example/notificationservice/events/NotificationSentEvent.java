package com.example.notificationservice.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSentEvent {
    private String transactionId;
    private BigDecimal amount;
    private String senderCustomerId;
    private LocalDateTime localDateTime;
    private String message;
    private String channel;
}

