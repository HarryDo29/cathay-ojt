package com.cathay.apigateway.interfaces;

import com.cathay.apigateway.entity.CircuitBreakerRuleEntity;
import reactor.core.publisher.Flux;

public interface ICircuitBreakerRuleRepository {
    public Flux<CircuitBreakerRuleEntity> loadAllowedHeaders();
}
