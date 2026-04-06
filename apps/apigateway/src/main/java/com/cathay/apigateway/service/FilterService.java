package com.cathay.apigateway.service;

import com.cathay.apigateway.entity.FilterEntity;
import com.cathay.apigateway.interfaces.IFilterRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.Set;

@Slf4j
@Service
public class FilterService {
    @Getter
    private volatile Set<FilterEntity> filters = Set.of();

    private final IFilterRepository routeFilterRepo;

    public FilterService(IFilterRepository routeFilterRepo){
        this.routeFilterRepo = routeFilterRepo;
    }

    @PostConstruct
    public void init() {
        log.info("[RouteFilter] ▶️ Loading gateway route filters...");
        loadFilters().block();
        log.info("[RouteFilter] ✅ Route filters ready — {} filters cached", filters.size());
    }

    public Mono<Void> loadFilters() {
        return routeFilterRepo.getAllFilters()
                .collectList()
                .doOnNext(list -> filters = Set.copyOf(list))
                .then();
    }
}
