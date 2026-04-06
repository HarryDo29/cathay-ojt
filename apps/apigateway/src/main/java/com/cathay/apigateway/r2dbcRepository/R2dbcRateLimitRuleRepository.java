package com.cathay.apigateway.r2dbcRepository;

import com.cathay.apigateway.entity.RateLimitRuleEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface R2dbcRateLimitRuleRepository extends
        ReactiveCrudRepository<RateLimitRuleEntity, UUID> {
}