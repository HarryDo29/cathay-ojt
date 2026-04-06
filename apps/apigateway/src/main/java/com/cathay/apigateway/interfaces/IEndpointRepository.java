package com.cathay.apigateway.interfaces;

import com.cathay.apigateway.entity.EndpointEntity;
import reactor.core.publisher.Flux;

public interface IEndpointRepository {
    public Flux<EndpointEntity> getAllEndpoints();
}
