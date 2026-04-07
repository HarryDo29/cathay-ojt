package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.MethodRulesConfig;
import com.cathay.apigateway.entity.MethodRuleEntity;
import com.cathay.apigateway.interfaces.IMethodRuleRepository;
import com.cathay.apigateway.r2dbcRepository.R2dbcMethodRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MethodRuleRepository implements IMethodRuleRepository {
    private final MethodRulesConfig methodRulesConfig;
    private final R2dbcMethodRuleRepository methodRuleRepo;

    @Override
    public Flux<MethodRuleEntity> getAllMethodRules() {
        List<MethodRulesConfig.MethodRule> yamlmethodRules = methodRulesConfig.getMethod_rules();
        return methodRuleRepo.findAll()
                .collectList()
                .flatMapMany(methodRules ->{
                    if (!methodRules.isEmpty()){
                        return Flux.fromIterable(methodRules);
                    }
                    return Flux.empty();
                })
                .onErrorResume(error -> {
                    // Log the error if needed
                    System.err.println("Error loading method rules from DB: " + error.getMessage());
                    // Fallback to YML configuration
                    return Flux.fromIterable(yamlmethodRules)
                            .map(config -> {
                                MethodRuleEntity entity = new MethodRuleEntity();
                                entity.setId(config.getId());
                                entity.setMethod(config.getMethod());
                                entity.setRequire_body(config.isRequire_body());
                                entity.setRequire_content_type(config.isRequire_content_type());
                                entity.setMax_body_size(config.getMax_body_size());
                                return entity;
                            });
                });
    }
}
