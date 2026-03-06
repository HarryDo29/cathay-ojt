package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.ServiceConfig;
import com.cathay.apigateway.entity.FilterEntity;
import com.cathay.apigateway.interfaces.IRouteFilterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class YmlRouteFilterRepository implements IRouteFilterRepository {
    private final ServiceConfig serviceConfig;

    @Override
    public Flux<FilterEntity> getAllFilters() {
        List<ServiceConfig.RouteFilters> filters = serviceConfig.getFilters();
        return Flux.fromIterable(filters)
                .map(filter -> {
                    if (filter.getStatus().equals("ACTIVE")) {
                        FilterEntity entity = new FilterEntity();
                        entity.setId(filter.getId());
                        entity.setName(filter.getName());
                        entity.setDescription(filter.getDescription());
                        entity.setStatus(filter.getStatus());
                        return entity;
                    }
                    return null;
                });
    }
}
