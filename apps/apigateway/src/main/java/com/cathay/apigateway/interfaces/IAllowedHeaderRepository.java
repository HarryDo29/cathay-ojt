package com.cathay.apigateway.interfaces;

import com.cathay.apigateway.entity.AllowedHeaderEntity;
import reactor.core.publisher.Flux;

public interface IAllowedHeaderRepository {
    public Flux<AllowedHeaderEntity> loadAllowedHeaders();
}
