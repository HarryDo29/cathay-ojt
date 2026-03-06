package com.cathay.apigateway.service;

import com.cathay.apigateway.entity.FilterEntity;
import com.cathay.apigateway.entity.ServiceFilterEntity;
import com.cathay.apigateway.interfaces.IServiceFilterRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class ServiceFilterService {
    private volatile Map<UUID, List<ServiceFilterEntity>> serviceFiltersMap = new HashMap<>();

    private final IServiceFilterRepository serviceFilterRepo;
    private final RouteFilterService routeFilterService;

    public ServiceFilterService(IServiceFilterRepository serviceFilterRepo, RouteFilterService routeFilterService) {
        this.serviceFilterRepo = serviceFilterRepo;
        this.routeFilterService = routeFilterService;
    }

    @PostConstruct
    public void init() {
        log.info("[Gateway] ▶️ Loading service-filter mappings...");
        loadServiceFilters().block();
        log.info("[Gateway] ✅ Service-filter mappings ready — {} services configured", serviceFiltersMap.size());
    }

    private Mono<Void> loadServiceFilters() {
        return serviceFilterRepo.getAllServiceFilters()
                .collectList()
                .doOnNext(list -> {
                    Map<UUID, List<ServiceFilterEntity>> map = new HashMap<>();
                    for (ServiceFilterEntity entity : list) {
                        map.computeIfAbsent(entity.getServiceId(), k -> new java.util.ArrayList<>())
                                .add(entity);
                    }
                    serviceFiltersMap = Map.copyOf(map);
                })
                .then();
    }

    public List<FilterEntity> getFiltersForService(UUID serviceId) {
        List<ServiceFilterEntity> list = new ArrayList<>(serviceFiltersMap.getOrDefault(serviceId, new ArrayList<>()));
        Set<FilterEntity> routeFilters = routeFilterService.getFilters();
        List<FilterEntity> serviceFilters = new java.util.ArrayList<>();
        list.sort(Comparator.comparingInt(ServiceFilterEntity::getSort_order));
        for (ServiceFilterEntity entity : list) {
            FilterEntity nEntity = routeFilters.stream()
                    .filter(filterEntity -> filterEntity.getId().equals(entity.getFilterId()))
                    .findFirst()
                    .orElse(null);
            if (nEntity != null) {
                serviceFilters.add(nEntity);
            }
        }
        return serviceFilters;
    }
}
