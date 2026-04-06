package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.HeaderRuleConfig;
import com.cathay.apigateway.entity.HeaderRuleEntity;
import com.cathay.apigateway.interfaces.IHeaderRuleRepository;
import com.cathay.apigateway.r2dbcRepository.R2dbcHeaderRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class HeaderRuleRepository implements IHeaderRuleRepository {
    private final HeaderRuleConfig allowedHeaderConfig;
    private final R2dbcHeaderRuleRepository headerRuleRepo;

    public Flux<HeaderRuleEntity> getHeaderRules() {
        List<HeaderRuleConfig.HeaderRule> yamlHeadersRule = allowedHeaderConfig.getHeader_rules();
        return headerRuleRepo.findAll()
                .collectList()
                .flatMapMany(headerRules -> {
                    if (!headerRules.isEmpty()){
                        return Flux.fromIterable(headerRules);
                    }
                    return Flux.empty();
                })
                .onErrorResume(error -> {
                    // Log the error if needed
                    System.err.println("Error loading header rules from DB: " + error.getMessage());
                    // Fallback to YML configuration
                    return Flux.fromIterable(yamlHeadersRule)
                            .map(allowedHeader -> {
                                HeaderRuleEntity entity = new HeaderRuleEntity();
                                entity.setId(allowedHeader.getId());
                                entity.setName(allowedHeader.getName());
                                entity.setMax_length(allowedHeader.getMax_length());
                                entity.setPattern(allowedHeader.getPattern());
                                entity.setDescription(allowedHeader.getDescription());
                                return entity;
                            });
                });
    }
}
