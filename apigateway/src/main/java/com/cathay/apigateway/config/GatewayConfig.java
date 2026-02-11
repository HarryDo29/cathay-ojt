package com.cathay.apigateway.config;

import com.cathay.apigateway.entity.ServiceEntity;
import com.cathay.apigateway.filter.AuthenticationGatewayFilterFactory;
import com.cathay.apigateway.service.RouteRegistryService;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Configuration
public class GatewayConfig {
    public final AuthenticationGatewayFilterFactory authenticationFilter;
    public final RouteRegistryService routeService;

    public GatewayConfig(AuthenticationGatewayFilterFactory authenticationFilter,
                         RouteRegistryService routeService) {
        this.authenticationFilter = authenticationFilter;
        this.routeService = routeService;
    }

    @Bean
    public RouteDefinitionLocator customLocator(){
       // ========================================
       // DYNAMIC CODE - Load from config
       // ========================================
       return() -> {
           Collection<ServiceEntity> services = routeService.getServiceCacheMap();
           log.info("🔍 RouteDefinitionLocator called. Services in cache: " + services.size());
           
           return Flux.fromIterable(services)
               .doOnNext(service ->
                       log.info("🛣️  Creating route for: " + service.getName()
                               + " | Path: " + service.getPath() + " | URL: " + service.getUrl()))
               .map(serviceEntity -> {
                   // Create RouteDefinition for each serviceEntity
                   RouteDefinition routeDefinition = new RouteDefinition();
                   routeDefinition.setId(serviceEntity.getName());
                   routeDefinition.setUri(java.net.URI.create(serviceEntity.getUrl())); // Target URL http://host:port
                   // Predicates for matching paths
                   PredicateDefinition predicate = new PredicateDefinition();
                   predicate.setName("Path"); // Match paths like /api/v1/{service}/**
                   predicate.addArg("pattern", serviceEntity.getPath());
                   routeDefinition.setPredicates(List.of(predicate));
                   // Filters for authentication and stripping prefix
                   FilterDefinition stripPrefixFilter = new FilterDefinition();
                   stripPrefixFilter.setName("StripPrefix"); // Remove first 3 segments: /api/v1/{service}/
                   stripPrefixFilter.addArg("parts", "3");
                   FilterDefinition authFilter = new FilterDefinition();
                   authFilter.setName("Authentication"); // Custom authentication filter
                   FilterDefinition authorFilter = new FilterDefinition();
                   authFilter.setName("Authorization"); // Custom authorization filter
                   routeDefinition.setFilters(List.of(authFilter, authorFilter, stripPrefixFilter));
                   return routeDefinition;
               });
       };
    }
}
