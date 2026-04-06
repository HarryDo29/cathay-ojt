package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.ServiceConfig;
import com.cathay.apigateway.entity.ServiceEntity;
import com.cathay.apigateway.interfaces.IServiceRepository;
import com.cathay.apigateway.r2dbcRepository.R2dbcServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ServiceRepository implements IServiceRepository {
    private final ServiceConfig serviceConfig;
    private final R2dbcServiceRepository serviceRepo;

    @Override
    public Flux<ServiceEntity> getAllServices() {
        List<ServiceConfig.RouteService> yamlServices = serviceConfig.getServices();
        return serviceRepo.findAll()
                .collectList()
                .flatMapMany(services -> {
                    if (!services.isEmpty()){
                        return Flux.fromIterable(services);
                    }
                    return Flux.empty();
                })
                .onErrorResume(error -> {
                    // Log the error if needed
                    System.err.println("Error loading services from DB: " + error.getMessage());
                    // Fallback to YML configuration
                    return Flux.fromIterable(yamlServices)
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
                });
    }
}
