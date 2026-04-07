package com.cathay.apigateway.service;

import com.cathay.apigateway.entity.CircuitBreakerRuleEntity;
import com.cathay.apigateway.interfaces.ICircuitBreakerRuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.EndpointAccessResolver;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
@Service
public class CircuitBreakerRuleService {
    private final ICircuitBreakerRuleRepository circuitBreakerRepository;
    public static final UUID DEFAULT_SERVICE_ID = UUID.fromString("ffc6346d-77d5-4306-8ff7-6fef886824c8");

    @Getter
    public volatile List<CircuitBreakerRuleEntity> circuitBreakerList = List.of();

    public CircuitBreakerRuleService(ICircuitBreakerRuleRepository circuitBreakerRepository, EndpointAccessResolver endpointAccessResolver) {
        this.circuitBreakerRepository = circuitBreakerRepository;
    }

    @PostConstruct
    public void init() {
        log.info("[CircuitHeader] ▶️ Loading Circuit Breaker Rules...");
        loadAllCircuitBreakers().block();
        log.info("[CircuitHeader] ✅ CircuitBreaker Rules ready — {} allowed headers active", circuitBreakerList.size());
    }

    public Mono<Void> loadAllCircuitBreakers() {
        return circuitBreakerRepository.loadAllowedHeaders()
                .collectList()
                .doOnNext(circuitBreakers -> {
                    circuitBreakerList = circuitBreakerList.stream()
                            .filter(CircuitBreakerRuleEntity::getEnabled)
                            .toList();
                })
                .then();
    }

    public Optional<CircuitBreakerRuleEntity> findCircuitBreakerById(UUID serviceId) {
        return circuitBreakerList.stream()
                .filter(cb -> Objects.equals(cb.getService_id(), serviceId))
                .findFirst();
    }

    public Optional<CircuitBreakerRuleEntity> findDefaultCircuitBreaker() {
        return circuitBreakerList.stream()
                .filter(cb -> Objects.equals(cb.getService_id(), DEFAULT_SERVICE_ID))
                .findFirst();
    }

}
