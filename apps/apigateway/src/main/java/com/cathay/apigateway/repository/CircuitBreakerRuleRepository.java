package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.CircuitBreakerConfig;
import com.cathay.apigateway.entity.CircuitBreakerRuleEntity;
import com.cathay.apigateway.interfaces.ICircuitBreakerRuleRepository;
import com.cathay.apigateway.r2dbcRepository.R2dbcCircuitBreakerRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CircuitBreakerRuleRepository implements ICircuitBreakerRuleRepository {
    private final CircuitBreakerConfig circuitBreakerConfig;
    private final R2dbcCircuitBreakerRuleRepository circuitBreakerRuleRepo;

    @Override
    public Flux<CircuitBreakerRuleEntity> loadAllowedHeaders() {
        List<CircuitBreakerConfig.CircuitBreakerRule> rules = circuitBreakerConfig.getRules();
        return circuitBreakerRuleRepo.findAll()
                .collectList()
                .flatMapMany(circuitBreakerRules -> {
                    if (!circuitBreakerRules.isEmpty()){
                        return Flux.fromIterable(circuitBreakerRules);
                    }
                    return Flux.empty();
                })
                .onErrorResume(error -> {
                    // Log the error if needed
                    System.err.println("Error loading circuit breaker rules from DB: " + error.getMessage());
                    // Fallback to YML configuration
                    return Flux.fromIterable(rules)
                            .map(rule -> {
                                CircuitBreakerRuleEntity entity = new CircuitBreakerRuleEntity();
                                entity.setId(rule.getId());
                                entity.setService_id(rule.getService_id());
                                entity.setEnabled(Boolean.parseBoolean(rule.getEnabled()));
                                entity.setFailure_rate_threshold(rule.getFailure_rate_threshold());
                                entity.setSlow_call_rate_threshold(rule.getSlow_call_rate_threshold());
                                entity.setSlow_call_duration_threshold(rule.getSlow_call_duration_threshold());
                                entity.setPermitted_number_of_calls_in_half_open_state(rule.getPermitted_number_of_calls_in_half_open_state());
                                entity.setSliding_window_type(rule.getSliding_window_type());
                                entity.setSliding_window_size(rule.getSliding_window_size());
                                entity.setMinimum_number_of_calls(rule.getMinimum_number_of_calls());
                                entity.setWait_duration_in_open_state(rule.getWait_duration_in_open_state());
                                entity.setName(rule.getName());
                                return entity;
                            });
                });
    }
}
