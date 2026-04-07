package com.cathay.apigateway.service;

import com.cathay.apigateway.core.routing.PathTrie;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GatewayConfigReloadService {
    private final ServiceRegistryService serviceRegistryService;
    private final EndpointService endpointService;
    private final FilterService filterService;
    private final ServiceFilterService serviceFilterService;
    private final AllowedOriginService allowedOriginService;
    private final AllowedHeaderService allowedHeaderService;
    private final MethodRuleService methodRuleService;
    private final HeaderRuleService headerRuleService;
    private final RoleService roleService;
    private final EndpointHeaderRuleService endpointHeaderRuleService;
    private final RateLimitRuleService rateLimitRuleService;
    private final CircuitBreakerRuleService circuitBreakerRuleService;
    private final PathTrie pathTrie;
    private final ApplicationEventPublisher eventPublisher;

    public Mono<Void> reloadAll() {
        // Load all data from DB first; only clear and apply cache if everything succeeds.
        // This prevents a partial/empty cache state when DB errors occur mid-reload.
        pathTrie.clear();
        return serviceRegistryService.loadServices()
                .then(endpointService.loadEndpoints())
                .then(filterService.loadFilters())
                .then(serviceFilterService.loadServiceFilters())
                .then(allowedOriginService.loadAllowedOrigins())
                .then(allowedHeaderService.loadAllowedHeaders())
                .then(methodRuleService.loadMethodRules())
                .then(headerRuleService.loadAllowedHeaders())
                .then(roleService.loadRoles())
                .then(endpointHeaderRuleService.loadEndpointHeaderRules())
                .then(rateLimitRuleService.loadRateLimits())
                .then(circuitBreakerRuleService.loadAllCircuitBreakers())
                .doOnSuccess(v -> {
                    eventPublisher.publishEvent(new RefreshRoutesEvent(this));
                });
    }
}
