package com.cathay.apigateway.interfaces;

import com.cathay.apigateway.entity.RoleEntity;
import reactor.core.publisher.Flux;

public interface IRoleRuleRepository {
    public Flux<RoleEntity> loadAllRoles();
}
