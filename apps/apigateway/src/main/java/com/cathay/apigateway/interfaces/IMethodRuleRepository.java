package com.cathay.apigateway.interfaces;

import com.cathay.apigateway.entity.MethodRuleEntity;
import reactor.core.publisher.Flux;

public interface IMethodRuleRepository {
    public Flux<MethodRuleEntity> getAllMethodRules();
}
