package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.ServiceConfig;
import com.cathay.apigateway.entity.ServiceEntity;
import com.cathay.apigateway.interfaces.IRouteServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class YmlRouteServiceRepository implements IRouteServiceRepository {
    private final ServiceConfig serviceConfig;

    @Override
    public Flux<ServiceEntity> getAllServices() {
        List<ServiceConfig.RouteService> services = serviceConfig.getServices();
        return Flux.fromIterable(services)
                .map(service -> {
                    if (Boolean.parseBoolean(service.getEnabled())) {
                        ServiceEntity entity = new ServiceEntity();
                        entity.setId(service.getId());
                        entity.setName(service.getName());
                        entity.setPath(service.getPath());
                        entity.setUrl(service.getUrl());
                        entity.setStrip_prefix(Integer.parseInt(service.getStrip_prefix()));
                        entity.setEnabled(true);
                        return entity;
                    }
                    return null;
                });
    }
}
