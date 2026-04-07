package com.cathay.apigateway.interfaces;

import com.cathay.apigateway.entity.RateLimitRuleEntity;
import reactor.core.publisher.Flux;

public interface IRateLimitRuleRepository {
    public Flux<RateLimitRuleEntity> getAllRateLimitRules();
}
