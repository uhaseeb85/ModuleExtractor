package com.example.payment.service;

import com.example.common.model.OrderEntity;
import com.example.common.model.UserEntity;
import com.example.payment.client.OrderServiceClient;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final OrderServiceClient orderClient;

    public PaymentService(OrderServiceClient orderClient) {
        this.orderClient = orderClient;
    }

    /**
     * Processes payment for a given order.
     * Imports OrderEntity and UserEntity cross-repo — intentional for test fixture.
     */
    public void processPayment(Long orderId, UserEntity payer) {
        OrderEntity order = orderClient.getOrder(orderId);
        // ... payment logic
        System.out.printf("Processing %s for user %s%n", order.getTotalAmount(), payer.getUsername());
    }
}
