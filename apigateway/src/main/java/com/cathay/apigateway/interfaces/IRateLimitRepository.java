package com.cathay.apigateway.interfaces;

import com.cathay.apigateway.entity.RateLimitEntity;
import reactor.core.publisher.Flux;

public interface IRateLimitRepository {
    public Flux<RateLimitEntity> getAllRateLimits();
}
