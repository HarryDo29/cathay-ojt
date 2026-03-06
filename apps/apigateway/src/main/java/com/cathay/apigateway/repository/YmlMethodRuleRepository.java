package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.MethodRulesConfig;
import com.cathay.apigateway.entity.MethodRuleEntity;
import com.cathay.apigateway.interfaces.IMethodRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class YmlMethodRuleRepository implements IMethodRuleRepository {
    private final MethodRulesConfig methodRulesConfig;

    @Override
    public Flux<MethodRuleEntity> getAllMethodRules() {
        List<MethodRulesConfig.MethodRule> methodRuleList = methodRulesConfig.getMethod_rules();
        return Flux.fromIterable(methodRuleList)
                .map(config -> {
                    MethodRuleEntity entity = new MethodRuleEntity();
                    entity.setId(config.getId());
                    entity.setMethod(config.getMethod());
                    entity.setRequire_body(config.isRequire_body());
                    entity.setRequire_content_type(config.isRequire_content_type());
                    entity.setMax_body_size(config.getMax_body_size());
                    return entity;
                });
    }
}
