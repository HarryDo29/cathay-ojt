package com.cathay.apigateway.service;

import com.cathay.apigateway.entity.FilterEntity;
import com.cathay.apigateway.interfaces.IRouteFilterRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class RouteFilterService {
    @Getter
    private volatile Set<FilterEntity> filters = Set.of();

    private final IRouteFilterRepository routeFilterRepo;

    public RouteFilterService(IRouteFilterRepository routeFilterRepo){
        this.routeFilterRepo = routeFilterRepo;
    }

    @PostConstruct
    public void init() {
        log.info("[Gateway] ▶️ Loading gateway route filters...");
        loadFilters().block();
        log.info("[Gateway] ✅ Route filters ready — {} filters cached", filters.size());
    }

    private Mono<Void> loadFilters() {
        return routeFilterRepo.getAllFilters()
                .collectList()
                .doOnNext(list -> filters = Set.copyOf(list))
                .then();
    }
}
