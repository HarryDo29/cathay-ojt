package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.HeaderRuleConfig;
import com.cathay.apigateway.entity.HeaderRulesEntity;
import com.cathay.apigateway.interfaces.IHeaderRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class YmlHeaderRulesRepository implements IHeaderRuleRepository {
    private final HeaderRuleConfig allowedHeaderConfig;

    @Override
    public Flux<HeaderRulesEntity> getAllAllowedHeaders() {
        List<HeaderRuleConfig.HeaderRule> allowedHeaders = allowedHeaderConfig.getHeader_rules();
        return Flux.fromIterable(allowedHeaders)
                .map(allowedHeader -> {
                    HeaderRulesEntity entity = new HeaderRulesEntity();
                    entity.setId(allowedHeader.getId());
                    entity.setName(allowedHeader.getName());
                    entity.setMax_length(allowedHeader.getMax_length());
                    entity.setPattern(allowedHeader.getPattern());
                    entity.setDescription(allowedHeader.getDescription());
                    return entity;
                });
    }
}
