package com.cathay.apigateway.interfaces;

import com.cathay.apigateway.entity.RoleEntity;
import reactor.core.publisher.Flux;

public interface IRoleRepository {
    public Flux<RoleEntity> loadAllRoles();
}
