package com.cathay.apigateway.interfaces;

import com.cathay.apigateway.entity.HeaderRuleEntity;
import reactor.core.publisher.Flux;

public interface IHeaderRuleRepository {
    public Flux<HeaderRuleEntity> getHeaderRules();
}
