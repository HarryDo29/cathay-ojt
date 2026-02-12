package com.cathay.apigateway.interfaces;

import com.cathay.apigateway.entity.AllowedOriginEntity;
import reactor.core.publisher.Flux;

public interface IAllowedOriginRepository {
    public Flux<AllowedOriginEntity> loadAllAllowedOrigins();
}
