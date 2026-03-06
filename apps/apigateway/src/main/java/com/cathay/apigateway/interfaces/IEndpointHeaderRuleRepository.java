package com.cathay.apigateway.interfaces;

import com.cathay.apigateway.entity.EndpointHeaderRuleEntity;
import reactor.core.publisher.Flux;

public interface IEndpointHeaderRuleRepository {
    public Flux<EndpointHeaderRuleEntity> loadAllEndpointHeaderRules();
}
