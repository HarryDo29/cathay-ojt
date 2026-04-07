package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.ServiceConfig;
import com.cathay.apigateway.entity.FilterEntity;
import com.cathay.apigateway.interfaces.IFilterRepository;
import com.cathay.apigateway.r2dbcRepository.R2dbcFilterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class FilterRepository implements IFilterRepository {
    private final ServiceConfig serviceConfig;
    private final R2dbcFilterRepository filterRepo;

    @Override
    public Flux<FilterEntity> getAllFilters() {
        List<ServiceConfig.RouteFilters> yamlFilters = serviceConfig.getFilters();
        return filterRepo.findAll()
                .collectList()
                .flatMapMany(filterList -> {
                    if (!filterList.isEmpty()){
                        return Flux.fromIterable(filterList);
                    }
                    return Flux.empty();
                })
                .onErrorResume(error -> {
                    // Log the error if needed
                    System.err.println("Error loading filters from DB: " + error.getMessage());
                    // Fallback to YML configuration
                    return Flux.fromIterable(yamlFilters)
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
                });
    }
}
