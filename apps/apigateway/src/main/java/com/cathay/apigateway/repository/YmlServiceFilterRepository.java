package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.ServiceConfig;
import com.cathay.apigateway.entity.ServiceFilterEntity;
import com.cathay.apigateway.interfaces.IServiceFilterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class YmlServiceFilterRepository implements IServiceFilterRepository {
    private final ServiceConfig serviceConfig;

    @Override
    public Flux<ServiceFilterEntity> getAllServiceFilters() {
        List<ServiceConfig.ServiceFilters> serviceFilters = serviceConfig.getServiceFilters();
        return Flux.fromIterable(serviceFilters)
                .map(serviceFilter -> {
                    if (Boolean.parseBoolean(serviceFilter.getEnabled())) {
                        ServiceFilterEntity entity = new ServiceFilterEntity();
                        entity.setId(serviceFilter.getId());
                        entity.setServiceId(serviceFilter.getServiceId());
                        entity.setFilterId(serviceFilter.getFilterId());
                        entity.setEnabled(serviceFilter.getEnabled());
                        entity.setSort_order(Integer.parseInt(serviceFilter.getSort_order()));
                        return entity;
                    }
                    return null;
                });
    }
}
