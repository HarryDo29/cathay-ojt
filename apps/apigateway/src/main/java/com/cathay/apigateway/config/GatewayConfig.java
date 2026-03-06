package com.cathay.apigateway.config;

import com.cathay.apigateway.entity.FilterEntity;
import com.cathay.apigateway.entity.ServiceEntity;
import com.cathay.apigateway.service.RouteRegistryService;
import com.cathay.apigateway.service.ServiceFilterService;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Configuration
public class GatewayConfig {
    private final RouteRegistryService routeService;
    private final ServiceFilterService serviceFilterService;

    public GatewayConfig(RouteRegistryService routeService, 
        ServiceFilterService serviceFilterService) {
        this.routeService = routeService;
        this.serviceFilterService = serviceFilterService;
    }

    @Bean
    public RouteDefinitionLocator customLocator(){
        // DYNAMIC CODE - Load from config
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
                   List<FilterDefinition> filters = new ArrayList<>(); // filter list
                   // list of filters for this service
                   List<FilterEntity> serviceFilters = serviceFilterService.getFiltersForService(serviceEntity.getId());
                   // Add filters based on service configuration
                   for (FilterEntity filter : serviceFilters) {
                       FilterDefinition filterDef = new FilterDefinition();
                       filterDef.setName(filter.getName());
                       filters.add(filterDef);
                   }
                   // Add StripPrefix filter if configured
                   if (serviceEntity.getStrip_prefix() > 0) {
                       FilterDefinition stripPrefixFilter = new FilterDefinition();
                       stripPrefixFilter.setName("StripPrefix"); // Remove first 3 segments: /api/v1/{service}/
                       stripPrefixFilter.addArg("parts", serviceEntity.getStrip_prefix().toString());
                       filters.add(stripPrefixFilter);
                   }
                   routeDefinition.setFilters(filters);
                   return routeDefinition;
               });
       };
    }
}
