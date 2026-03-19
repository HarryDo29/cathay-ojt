package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.CircuitBreakerConfig;
import com.cathay.apigateway.entity.CircuitBreakerEntity;
import com.cathay.apigateway.entity.ServiceEntity;
import com.cathay.apigateway.interfaces.ICircuitBreakerRuleRepository;
import com.cathay.apigateway.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class YmlCircuitBreakerRepository implements ICircuitBreakerRuleRepository {
    private final CircuitBreakerConfig circuitBreakerConfig;

    @Override
    public Flux<CircuitBreakerEntity> loadAllowedHeaders() {
        List<CircuitBreakerConfig.CircuitBreakerRule> rules = circuitBreakerConfig.getRules();
        return Flux.fromIterable(rules)
                .map(rule -> {
                    CircuitBreakerEntity entity = new CircuitBreakerEntity();
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
    }
}
