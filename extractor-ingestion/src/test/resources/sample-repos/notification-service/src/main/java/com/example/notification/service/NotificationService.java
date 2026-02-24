package com.example.notification.service;

import com.example.common.model.UserEntity;
import org.springframework.stereotype.Service;

/**
 * Sends notifications to users. Imports UserEntity directly (cross-repo coupling).
 */
@Service
public class NotificationService {

    public void sendWelcomeEmail(UserEntity user) {
        System.out.printf("Sending welcome email to %s%n", user.getEmail());
    }

    public void sendOrderConfirmation(UserEntity user, Long orderId) {
        System.out.printf("Order %d confirmed for %s%n", orderId, user.getEmail());
    }
}
