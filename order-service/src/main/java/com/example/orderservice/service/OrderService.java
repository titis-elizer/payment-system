package com.example.orderservice.service;

import com.example.orderservice.entity.Order;
import com.example.orderservice.events.OrderCreatedEvent;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Order create(Order order) {
        order.setStatus("PENDING");
        Order saved = repository.save(order);

        kafkaTemplate.send(
                "order-created",
                new OrderCreatedEvent(saved.getId(), saved.getAmount(), saved.getCustomerId())
        );

        return saved;
    }
}

