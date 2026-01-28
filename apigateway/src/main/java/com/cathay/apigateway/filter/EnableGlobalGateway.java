package com.cathay.apigateway.filter;

import com.cathay.apigateway.entity.EndpointsEntity;
import com.cathay.apigateway.service.EndpointRegisterService;
import com.cathay.apigateway.util.ErrorHandler;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class EnableGlobalGateway implements GlobalFilter, Ordered {

    private final EndpointRegisterService endpointRegisterService;
    private final ErrorHandler errorHandler;

    public EnableGlobalGateway(EndpointRegisterService endpointRegisterService,
                               ErrorHandler errorHandler) {
        this.endpointRegisterService = endpointRegisterService;
        this.errorHandler = errorHandler;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().toString();
        EndpointsEntity endpoint = endpointRegisterService.isEnabled(path);

        if (endpoint == null || !endpoint.isEnabled()) {
            return errorHandler.writeError(
                    exchange,
                    new NotFoundException("Endpoint not found"),
                    HttpStatus.NOT_FOUND
            );
        }

        if (!endpoint.getMethod().name().equalsIgnoreCase(method)) {
            return errorHandler.writeError(
                    exchange,
                    new NotFoundException("Endpoint not found"),
                    HttpStatus.NOT_FOUND
            );
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    public static class Config {}
}
