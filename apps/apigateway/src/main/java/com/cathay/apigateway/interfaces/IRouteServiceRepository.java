package com.cathay.apigateway.interfaces;

import com.cathay.apigateway.entity.ServiceEntity;
import reactor.core.publisher.Flux;

public interface IRouteServiceRepository {
    Flux<ServiceEntity> getAllServices();
}
