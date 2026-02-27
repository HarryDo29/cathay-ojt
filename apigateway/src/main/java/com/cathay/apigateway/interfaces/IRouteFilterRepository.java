package com.cathay.apigateway.interfaces;

import com.cathay.apigateway.entity.FilterEntity;
import reactor.core.publisher.Flux;

public interface IRouteFilterRepository {
    Flux<FilterEntity> getAllFilters();
}
