package com.cathay.apigateway.r2dbcRepository;

import com.cathay.apigateway.entity.AllowedOriginEntity;
import com.cathay.apigateway.entity.ServiceFilterEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface R2dbcServiceFilterRepository extends
        ReactiveCrudRepository<ServiceFilterEntity, UUID> {
}