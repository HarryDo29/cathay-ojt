package com.cathay.apigateway.service;

import com.cathay.apigateway.entity.RoleEntity;
import com.cathay.apigateway.interfaces.IRoleRuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RoleService {
    private final IRoleRuleRepository roleRuleRepository;


    /** Quick lookup: role name (e.g. "ADMIN") → level (e.g. 4) */
    @Getter
    private volatile Map<String, RoleEntity> roleLevelMap = Map.of();

    public RoleService(IRoleRuleRepository roleRuleRepository) {
        this.roleRuleRepository = roleRuleRepository;
    }

    @PostConstruct
    public void init() {
        log.info("🔧 Load role into cache");
        this.loadRoles().block();
    }

    public Mono<Void> loadRoles() {
        return roleRuleRepository.loadAllRoles()
                .collectList()
                .doOnNext(list -> {
                    roleLevelMap = Map.copyOf(
                            list.stream()
                                    .filter(RoleEntity::isEnabled)
                                    .collect(Collectors.toMap(
                                            RoleEntity::getName,
                                            role -> role
                                    ))
                    );
                    log.info("✅ Loaded {} roles: {}", roleLevelMap.size(), roleLevelMap);
                })
                .then();
    }

    /**
     * Get the level for a given role name.
     * Returns 0 if role not found (no access).
     */
    public RoleEntity getRoleLevel(String roleName) {
        return roleLevelMap.getOrDefault(roleName, null);
    }
}
