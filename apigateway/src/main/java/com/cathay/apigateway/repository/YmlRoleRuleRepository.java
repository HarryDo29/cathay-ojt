package com.cathay.apigateway.repository;
import com.cathay.apigateway.data.config.RoleConfig;
import com.cathay.apigateway.entity.RoleEntity;
import com.cathay.apigateway.interfaces.IRoleRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class YmlRoleRuleRepository implements IRoleRuleRepository {
    private final RoleConfig roleConfig;

    @Override
    public Flux<RoleEntity> loadAllRoles() {
        List<RoleConfig.Role> roles = roleConfig.getRoles();
        return Flux.fromIterable(roles)
                .map(role -> {;
                    RoleEntity entity = new RoleEntity();
                    entity.setId(role.getId());
                    entity.setName(role.getName());
                    entity.setDescription(role.getDescription());
                    entity.setLevel(role.getLevel());
                    entity.setEnabled(Boolean.parseBoolean(role.getEnabled()));
                    return entity;
                });
    }
}
