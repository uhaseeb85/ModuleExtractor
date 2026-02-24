package com.example.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter: adds a correlation-id header to all upstream requests.
 * api-gateway is a standalone service with no cross-repo entity imports — used
 * as a "clean" baseline in tests.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = java.util.UUID.randomUUID().toString();
        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.header("X-Correlation-Id", correlationId))
                .build();
        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
