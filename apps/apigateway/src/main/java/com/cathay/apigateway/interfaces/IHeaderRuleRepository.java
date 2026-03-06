package com.cathay.apigateway.interfaces;

import com.cathay.apigateway.entity.HeaderRulesEntity;
import reactor.core.publisher.Flux;

public interface IHeaderRuleRepository {
    public Flux<HeaderRulesEntity> getAllAllowedHeaders();
}
