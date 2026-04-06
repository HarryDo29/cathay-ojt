package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.EndpointConfig;
import com.cathay.apigateway.entity.EndpointEntity;
import com.cathay.apigateway.interfaces.IEndpointRepository;
import com.cathay.apigateway.r2dbcRepository.R2dbcEndpointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EndpointRepository implements IEndpointRepository {
    private final EndpointConfig endpointConfig;
    private final R2dbcEndpointRepository endpointRepo;

    @Override
    public Flux<EndpointEntity> getAllEndpoints() {
        List<EndpointConfig.Endpoint> yamlEndpoints = endpointConfig.getEndpoints();
        return endpointRepo.findAll()
                .collectList()
                .flatMapMany(endpointList -> {
                    if (!endpointList.isEmpty()){
                        return Flux.fromIterable(endpointList);
                    }
                    return Flux.empty();
                })
                .onErrorResume(error -> {
                    // Log the error if needed
                    System.err.println("Error loading endpoints from DB: " + error.getMessage());
                    // Fallback to YML configuration
                    return Flux.fromIterable(yamlEndpoints)
                            .map(endpoint -> {
                                EndpointEntity entity = new EndpointEntity();
                                entity.setId(endpoint.getId());
                                entity.setPath(endpoint.getPath());
                                entity.setMethod(endpoint.getMethod());
                                entity.setServiceId(endpoint.getServiceId());
                                entity.setEnabled(Boolean.parseBoolean(endpoint.getEnabled()));
                                entity.setPublic(Boolean.parseBoolean(endpoint.getIsPublic()));
                                entity.setMinRoleLevel(endpoint.getMin_role_level());
                                return entity;
                            });
                });
    }

}
