package com.cathay.apigateway.interfaces;

import com.cathay.apigateway.entity.EndpointsEntity;
import reactor.core.publisher.Flux;

public interface IEndpointServiceRepository {
    public Flux<EndpointsEntity> getAllEndpoints();
}
