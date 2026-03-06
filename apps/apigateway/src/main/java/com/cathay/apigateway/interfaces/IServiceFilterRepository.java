package com.cathay.apigateway.interfaces;

import com.cathay.apigateway.entity.ServiceFilterEntity;
import reactor.core.publisher.Flux;

public interface IServiceFilterRepository {
    Flux<ServiceFilterEntity> getAllServiceFilters();
}
