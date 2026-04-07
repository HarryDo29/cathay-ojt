package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.RoleConfig;
import com.cathay.apigateway.entity.RoleEntity;
import com.cathay.apigateway.interfaces.IRoleRepository;
import com.cathay.apigateway.r2dbcRepository.R2dbcRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RoleRepository implements IRoleRepository {
    private final RoleConfig roleConfig;
    private final R2dbcRoleRepository roleRepo;

    @Override
    public Flux<RoleEntity> loadAllRoles() {
        List<RoleConfig.Role> yamlRoles = roleConfig.getRoles();
        return roleRepo.findAll()
                .collectList()
                .flatMapMany(roles -> {
                    if (!roles.isEmpty()){
                        return Flux.fromIterable(roles);
                    }
                    return Flux.empty();
                })
                .onErrorResume(error -> {
                    // Log the error if needed
                    System.err.println("Error loading roles from DB: " + error.getMessage());
                    // Fallback to YML configuration
                    return Flux.fromIterable(yamlRoles)
                            .map(role -> {;
                                RoleEntity entity = new RoleEntity();
                                entity.setId(role.getId());
                                entity.setName(role.getName());
                                entity.setDescription(role.getDescription());
                                entity.setLevel(role.getLevel());
                                entity.setEnabled(Boolean.parseBoolean(role.getEnabled()));
                                return entity;
                            });
                });
    }
}
