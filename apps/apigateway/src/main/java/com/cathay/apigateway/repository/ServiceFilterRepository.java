package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.ServiceConfig;
import com.cathay.apigateway.entity.ServiceFilterEntity;
import com.cathay.apigateway.interfaces.IServiceFilterRepository;
import com.cathay.apigateway.r2dbcRepository.R2dbcServiceFilterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ServiceFilterRepository implements IServiceFilterRepository {
    private final ServiceConfig serviceConfig;
    private final R2dbcServiceFilterRepository serviceFilterRepo;

    @Override
    public Flux<ServiceFilterEntity> getAllServiceFilters() {
        List<ServiceConfig.ServiceFilters> yamlServiceFilters = serviceConfig.getServiceFilters();
        return serviceFilterRepo.findAll()
                .collectList()
                .flatMapMany(serviceFilterLists -> {
                    if (!serviceFilterLists.isEmpty()){
                        return Flux.fromIterable(serviceFilterLists);
                    }
                    return Flux.empty();
                })
                .onErrorResume(error -> {
                    // Log the error if needed
                    System.err.println("Error loading service filters from DB: " + error.getMessage());
                    // Fallback to YML configuration
                    return Flux.fromIterable(yamlServiceFilters)
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
                });
    }
}
