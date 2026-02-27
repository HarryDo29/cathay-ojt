package com.cathay.apigateway.service;

import com.cathay.apigateway.entity.MethodRuleEntity;
import com.cathay.apigateway.interfaces.IMethodRuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Set;

@Slf4j
@Service
public class MethodRuleService {
    @Getter
    private volatile Set<MethodRuleEntity> methodRuleSet = Set.of();

    private final IMethodRuleRepository methodRuleRepo;

    public MethodRuleService(IMethodRuleRepository methodRuleRepo) {
        this.methodRuleRepo = methodRuleRepo;
    }

    @PostConstruct
    public void init() {
        log.info("[Gateway] ▶️ Loading HTTP method rules...");
        loadMethodRules().block();
        log.info("[Gateway] ✅ Method rules ready — {} rules configured", methodRuleSet.size());
    }

    public Mono<Void> loadMethodRules() {
        return methodRuleRepo.getAllMethodRules()
                .collectList()
                .doOnNext(methodRuleList -> methodRuleSet = Set.copyOf(methodRuleList))
                .then();
    }

    public MethodRuleEntity getMethodRule(String method) {
        return methodRuleSet.stream()
                .filter(rule -> rule.getMethod().equalsIgnoreCase(method))
                .findFirst()
                .get();
    }
    
}
