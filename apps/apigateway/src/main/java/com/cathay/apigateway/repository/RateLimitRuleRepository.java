package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.RateLimitConfig;
import com.cathay.apigateway.entity.RateLimitRuleEntity;
import com.cathay.apigateway.enums.KeyType;
import com.cathay.apigateway.enums.RateLimitType;
import com.cathay.apigateway.interfaces.IRateLimitRuleRepository;
import com.cathay.apigateway.r2dbcRepository.R2dbcRateLimitRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RateLimitRuleRepository implements IRateLimitRuleRepository {
    private final RateLimitConfig rateLimitConfig;
    private final R2dbcRateLimitRuleRepository rateLimitRuleRepo;

    @Override
    public Flux<RateLimitRuleEntity> getAllRateLimitRules() {
        List<RateLimitConfig.RateLimit> yamlRateLimitRules = rateLimitConfig.getRules();
        return rateLimitRuleRepo.findAll()
                .collectList()
                .flatMapMany(rateLimitRules -> {
                    if (!rateLimitRules.isEmpty()){
                        return Flux.fromIterable(rateLimitRules);
                    }
                    return Flux.empty();
                })
                .onErrorResume(error -> {
                    // Log the error if needed
                    System.err.println("Error loading rate limit rules from DB: " + error.getMessage());
                    // Fallback to YML configuration
                    return Flux.fromIterable(yamlRateLimitRules)
                            .map(rule -> {
                                RateLimitRuleEntity entity = new RateLimitRuleEntity();
                                entity.setId(rule.getId());
                                entity.setType(RateLimitType.valueOf(rule.getType().toUpperCase()));
                                entity.setKeyType(KeyType.valueOf(rule.getKey_type().toUpperCase()));
                                entity.setRule(rule.getRule());
                                entity.setEnabled(Boolean.parseBoolean(rule.getEnabled()));
                                return entity;
                            });
                });
    }
}
