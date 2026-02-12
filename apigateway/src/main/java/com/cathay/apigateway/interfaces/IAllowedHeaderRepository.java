package com.cathay.apigateway.interfaces;

import com.cathay.apigateway.entity.AllowedHeaderEntity;
import com.cathay.apigateway.entity.EndpointHeaderRuleEntity;
import reactor.core.publisher.Flux;

public interface IAllowedHeaderRepository {
    public Flux<AllowedHeaderEntity> loadAllowedHeaders();
}
