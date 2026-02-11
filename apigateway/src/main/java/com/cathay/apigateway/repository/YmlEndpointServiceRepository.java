package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.EndpointConfig;
import com.cathay.apigateway.entity.EndpointsEntity;
import com.cathay.apigateway.interfaces.IEndpointServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class YmlEndpointServiceRepository implements IEndpointServiceRepository {
    private final EndpointConfig endpointConfig;

    @Override
    public Flux<EndpointsEntity> getAllEndpoints() {
        List<EndpointConfig.Endpoint> endpoints = endpointConfig.getEndpoints();
        return Flux.fromIterable(endpoints)
                .map(endpoint -> {
                    EndpointsEntity entity = new EndpointsEntity();
                    entity.setId(endpoint.getId());
                    entity.setPath(endpoint.getPath());
                    entity.setMethod(endpoint.getMethod());
                    entity.setServiceId(endpoint.getServiceId());
                    entity.setEnabled(Boolean.parseBoolean(endpoint.getEnabled()));
                    entity.setPublic(Boolean.parseBoolean(endpoint.getIsPublic()));
                    entity.setMinRoleLevel(endpoint.getMin_role_level());
                    return entity;
                });
    }
}
