package com.cathay.apigateway.service;

import com.cathay.apigateway.entity.CircuitBreakerEntity;
import com.cathay.apigateway.interfaces.ICircuitBreakerRuleRepository;
import com.cathay.apigateway.repository.YmlCircuitBreakerRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
        log.info("[Gateway] ▶️ Loading Circuit Breaker Rules...");
        loadAllCircuitBreakers().block();
        log.info("[Gateway] ✅ CircuitBreaker Rules ready — {} allowed headers active", circuitBreakerList.size());
    }

    public Mono<Void> loadAllCircuitBreakers() {
        return circuitBreakerRepository.loadAllowedHeaders()
                .collectList()
                .doOnNext(circuitBreakers -> {
                    circuitBreakerList = new ArrayList<>(circuitBreakers)
                            .stream()
                            .filter(CircuitBreakerEntity::getEnabled)
                            .toList();
                })
                .then();
    }
}
