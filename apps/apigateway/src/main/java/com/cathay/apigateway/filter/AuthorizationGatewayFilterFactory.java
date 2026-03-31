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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.nio.file.AccessDeniedException;

@Slf4j
@Component
public class AuthorizationGatewayFilterFactory extends
        AbstractGatewayFilterFactory<AuthorizationGatewayFilterFactory.Config> {

    private final EndpointRegisterService endpointRegisterService;
    private final RoleService roleService;
    private final ErrorHandler errorHandler;

    public AuthorizationGatewayFilterFactory(EndpointRegisterService endpointRegisterService,
                                             RoleService roleService, ErrorHandler errorHandler) {
        super(Config.class);
        this.endpointRegisterService = endpointRegisterService;
        this.roleService = roleService;
        this.errorHandler = errorHandler;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            val path = exchange.getRequest().getURI().getPath();
            val method = exchange.getRequest().getMethod();
            // find endpoint entity by path and method
            EndpointsEntity endpoint = endpointRegisterService
                            .getEndpoint(path, method.toString())
                            .getEntity();
            // endpoint is public, skip authorization
            if (endpoint != null && endpoint.isPublic()) {
                log.info("\uD83D\uDD12 Skipping authorization for public endpoint: {}", path);
                return chain.filter(exchange);
            }
            // get user ROLE from req header
            String role = exchange.getRequest().getHeaders().getFirst("X-User-Role");
            if (role == null || role.isEmpty()) {
                return errorHandler.writeJsonError(exchange,
                        HttpStatus.UNAUTHORIZED,
                        path,
                        "Unauthorized",
                        "Missing user role");
            }
            // resolve endpoint's roles and check with user ROLE
            RoleEntity roleEntity = roleService.getRoleLevel(role);
            if (roleEntity == null) {
                return errorHandler.writeJsonError(exchange,
                        HttpStatus.FORBIDDEN,
                        path,
                        "Forbidden",
                        "Invalid user role");
            }
            // check permission of user with endpoint's level
            if (roleEntity.getLevel() < endpoint.getMinRoleLevel()) {
                return errorHandler.writeJsonError(exchange,
                        HttpStatus.FORBIDDEN,
                        path,
                        "Forbidden",
                        "Insufficient role level");
            }
            return chain.filter(exchange);
        };
    }

    public static class Config {
    }
}
