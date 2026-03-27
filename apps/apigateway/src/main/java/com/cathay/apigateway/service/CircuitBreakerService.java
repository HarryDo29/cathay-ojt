package com.cathay.apigateway.service;

import com.cathay.apigateway.entity.CircuitBreakerEntity;
import com.cathay.apigateway.interfaces.ICircuitBreakerRuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CircuitBreakerService {
    private final ICircuitBreakerRuleRepository circuitBreakerRepository;

    @Getter
    public volatile List<CircuitBreakerEntity> circuitBreakerList = List.of();

    public CircuitBreakerService(ICircuitBreakerRuleRepository circuitBreakerRepository) {
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
                    circuitBreakerList = new ArrayList<>(circuitBreakers)
                            .stream()
                            .filter(CircuitBreakerEntity::isEnabled)
                            .toList();
                })
                .then();
    }

    public CircuitBreakerEntity getCircuitBreakerById(UUID service_id) {
        return circuitBreakerList.stream()
                .filter(cb -> cb.getService_id() != null)
                .filter(cb -> cb.getService_id().equals(service_id))
                .findFirst()
                .orElse(circuitBreakerList.getFirst());
    }
}
