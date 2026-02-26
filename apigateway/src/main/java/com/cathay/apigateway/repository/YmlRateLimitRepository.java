package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.RateLimitConfig;
import com.cathay.apigateway.entity.RateLimitEntity;
import com.cathay.apigateway.enums.KeyType;
import com.cathay.apigateway.interfaces.IRateLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class YmlRateLimitRepository implements IRateLimitRepository {
    private final RateLimitConfig rateLimitConfig;

    @Override
    public Flux<RateLimitEntity> getAllRateLimits() {
        List<RateLimitConfig.RateLimit> rules = rateLimitConfig.getRules();
        return Flux.fromIterable(rules)
                .map(rule -> {
                    RateLimitEntity entity = new RateLimitEntity();
                    entity.setId(rule.getId());
                    entity.setKeyType(KeyType.valueOf(rule.getKey_type().toUpperCase()));
                    entity.setReplenishRate(rule.getReplenish_rate());
                    entity.setBurstCapacity(rule.getBurst_capacity());
                    entity.setTtl(rule.getTtl());
                    entity.setEnabled("Y".equalsIgnoreCase(rule.getEnabled()));
                    return entity;
                });
    }
}
