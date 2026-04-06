package com.cathay.apigateway.interfaces;

import com.cathay.apigateway.entity.ServiceEntity;
import reactor.core.publisher.Flux;

public interface IServiceRepository {
    Flux<ServiceEntity> getAllServices();
}
