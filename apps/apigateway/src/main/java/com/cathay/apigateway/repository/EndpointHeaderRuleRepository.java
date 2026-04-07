package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.HeaderRuleConfig;
import com.cathay.apigateway.entity.EndpointHeaderRuleEntity;
import com.cathay.apigateway.interfaces.IEndpointHeaderRuleRepository;
import com.cathay.apigateway.r2dbcRepository.R2dbcEndpointHeaderRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EndpointHeaderRuleRepository implements IEndpointHeaderRuleRepository {
    private final HeaderRuleConfig headerRuleConfig;
    private final R2dbcEndpointHeaderRuleRepository endpointHeaderRuleRepo;

    @Override
    public Flux<EndpointHeaderRuleEntity> loadAllEndpointHeaderRules() {
        List<HeaderRuleConfig.EndpointHeaderRule> yamlEndpointHeaderRules = headerRuleConfig.getEndpoint_header_rules();
        return endpointHeaderRuleRepo.findAll()
                .collectList()
                .flatMapMany(endpointHeaderRules -> {
                    if (!endpointHeaderRules.isEmpty()){
                        return Flux.fromIterable(endpointHeaderRules);
                    }
                    return Flux.empty();
                })
                .onErrorResume(error -> {
                    // Log the error if needed
                    System.err.println("Error loading endpoints header rules from DB: " + error.getMessage());
                    // Fallback to YML configuration
                    return Flux.fromIterable(yamlEndpointHeaderRules)
                            .map(endpointHeaderRule -> {
                                EndpointHeaderRuleEntity entity = new EndpointHeaderRuleEntity();
                                entity.setId(endpointHeaderRule.getId());
                                entity.setEndpoint_id(endpointHeaderRule.getEndpoint_id());
                                entity.setHeader_rule_id(endpointHeaderRule.getHeader_rule_id());
                                entity.setRequired(Boolean.parseBoolean(endpointHeaderRule.getRequired()));
                                return entity;
                            });
                });
    }
}
