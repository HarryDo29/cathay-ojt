package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.HeaderRuleConfig;
import com.cathay.apigateway.entity.EndpointHeaderRuleEntity;
import com.cathay.apigateway.interfaces.IEndpointHeaderRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class YmlEndpointHeaderRuleRepository implements IEndpointHeaderRuleRepository {
    private final HeaderRuleConfig headerRuleConfig;

    @Override
    public Flux<EndpointHeaderRuleEntity> loadAllEndpointHeaderRules() {
        List<HeaderRuleConfig.EndpointHeaderRule> endpointHeaderRules = headerRuleConfig.getEndpoint_header_rules();
        return Flux.fromIterable(endpointHeaderRules)
                .map(endpointHeaderRule -> {
                    EndpointHeaderRuleEntity entity = new EndpointHeaderRuleEntity();
                    entity.setId(endpointHeaderRule.getId());
                    entity.setEndpoint_id(endpointHeaderRule.getEndpoint_id());
                    entity.setHeader_rule_id(endpointHeaderRule.getHeader_rule_id());
                    entity.setRequired(Boolean.parseBoolean(endpointHeaderRule.getRequired()));
                    return entity;
                });
    }
}
