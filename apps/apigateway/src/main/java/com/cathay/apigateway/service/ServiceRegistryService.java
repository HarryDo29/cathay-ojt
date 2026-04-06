package com.cathay.apigateway.service;

import com.cathay.apigateway.entity.ServiceEntity;
import com.cathay.apigateway.interfaces.IServiceRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ServiceRegistryService {
    private volatile Map<UUID, ServiceEntity> serviceCache = Map.of();

    private final IServiceRepository serviceRepo;

    public ServiceRegistryService(IServiceRepository serviceRepo) {
        this.serviceRepo = serviceRepo;
    }
    
    @PostConstruct
    public void init() {
        log.info("[Service] ▶️ Loading service route definitions...");
        loadServices().block();
        log.info("[Service] ✅ Route registry ready — {} service routes cached", serviceCache.size());
    }

    public Mono<Void> loadServices(){
        return serviceRepo.getAllServices()
                .doOnNext(service ->
                        log.debug("[Gateway]   Route registered: {} → {}", service.getName(), service.getPath()))
                .collectMap(ServiceEntity::getId)
                .doOnNext(map -> serviceCache = Map.copyOf(map))
                .then();
    }

    public Collection<ServiceEntity> getServiceCacheMap() {
        return serviceCache.values();
    }
}
