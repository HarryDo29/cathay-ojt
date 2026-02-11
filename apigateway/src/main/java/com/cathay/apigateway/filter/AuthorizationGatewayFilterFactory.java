package com.cathay.apigateway.filter;

import com.cathay.apigateway.entity.EndpointsEntity;
import com.cathay.apigateway.entity.RoleEntity;
import com.cathay.apigateway.service.EndpointRegisterService;
import com.cathay.apigateway.service.RoleService;
import com.cathay.apigateway.util.ErrorHandler;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.nio.file.AccessDeniedException;

@Slf4j
@Component
public class AuthorizationGatewayFilterFactory extends
        AbstractGatewayFilterFactory<AuthorizationGatewayFilterFactory.AuthorConfig>
        implements Ordered {

    private final EndpointRegisterService endpointRegisterService;
    private final RoleService roleService;
    private final ErrorHandler errorHandler;

    public AuthorizationGatewayFilterFactory(EndpointRegisterService endpointRegisterService,
                                             RoleService roleService, ErrorHandler errorHandler) {
        super(AuthorConfig.class);
        this.endpointRegisterService = endpointRegisterService;
        this.roleService = roleService;
        this.errorHandler = errorHandler;
    }

    @Override
    public GatewayFilter apply(AuthorConfig config) {
        return (exchange, chain) -> {
            val path = exchange.getRequest().getURI().getPath();
            val method = exchange.getRequest().getMethod();
            // find endpoint entity by path and method
            EndpointsEntity endpoint = endpointRegisterService
                            .getEndpoint(path, method.toString())
                            .getEntity();
            // endpoint is public, skip authorization
            if (endpoint != null && endpoint.isPublic()) {
                return chain.filter(exchange);
            }
            // get user ROLE from req header
            String role = exchange.getRequest().getHeaders().getFirst("X-User-Role");
            if (role == null || role.isEmpty()) {
                return errorHandler.writeError(exchange,
                        new AccessDeniedException("Missing user role"),
                        HttpStatus.UNAUTHORIZED);
            }
            // resolve endpoint's roles and check with user ROLE
            RoleEntity roleEntity = roleService.getRoleLevel(role);
            if (roleEntity == null) {
                return errorHandler.writeError(exchange,
                        new AccessDeniedException("Invalid user role"),
                        HttpStatus.FORBIDDEN);
            }
            // check permission of user with endpoint's level
            if (roleEntity.getLevel() < endpoint.getMinRoleLevel()) {
                return errorHandler.writeError(exchange,
                        new AccessDeniedException("Insufficient role level"),
                        HttpStatus.FORBIDDEN);
            }
            return chain.filter(exchange);
        };
    }

    public static class AuthorConfig {
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
