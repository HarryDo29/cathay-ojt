package com.cathay.apigateway.service;

import com.cathay.apigateway.entity.ServiceEntity;
import com.cathay.apigateway.interfaces.IRouteServiceRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class RouteRegistryService {
    private volatile Map<UUID, ServiceEntity> serviceCache = Map.of();

    private final IRouteServiceRepository serviceRepo;

    public RouteRegistryService(IRouteServiceRepository serviceRepo) {
        this.serviceRepo = serviceRepo;
    }
    
    @PostConstruct
    public void init() {
        log.info("🔧 RouteRegistryService @PostConstruct: Loading services...");
        loadServices().block(); // Load synchronously during bean initialization
        log.info("✅ RouteRegistryService initialized with " + serviceCache.size() + " services");
    }

    public Mono<Void> loadServices(){
        return serviceRepo.getAllServices()
                .doOnNext(service ->
                        log.info("📦 Loading service: " + service.getName() + " -> " + service.getPath()))
                .collectMap(ServiceEntity::getId)
                .doOnNext(map -> {
                    serviceCache = Map.copyOf(map);
                    log.info("✅ Total services loaded into cache: " + map.size());
                })
                .then();
    }

    public Collection<ServiceEntity> getServiceCacheMap() {
        return serviceCache.values();
    }
}
