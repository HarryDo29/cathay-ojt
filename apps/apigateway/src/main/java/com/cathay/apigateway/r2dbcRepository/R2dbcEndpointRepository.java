package com.cathay.apigateway.r2dbcRepository;

import com.cathay.apigateway.entity.EndpointEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface R2dbcEndpointRepository extends
        ReactiveCrudRepository<EndpointEntity, UUID> {
}