package com.example.payment.client;

import com.example.common.model.OrderEntity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client that crosses into the common-models domain — the shared entity antipattern.
 */
@FeignClient(name = "order-service", url = "${order.service.url}")
public interface OrderServiceClient {

    @GetMapping("/orders/{id}")
    OrderEntity getOrder(@PathVariable Long id);
}
