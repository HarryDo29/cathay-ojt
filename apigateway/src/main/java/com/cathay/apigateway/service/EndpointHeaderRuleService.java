package com.cathay.apigateway.service;

import com.cathay.apigateway.entity.EndpointHeaderRuleEntity;
import com.cathay.apigateway.interfaces.IEndpointHeaderRuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EndpointHeaderRuleService {
    private final IEndpointHeaderRuleRepository endpointHeaderRuleRepo;

    @Getter
    private volatile Map<String, List<EndpointHeaderRuleEntity>> endpointHeaderRule = Map.of();

    public EndpointHeaderRuleService(IEndpointHeaderRuleRepository endpointHeaderRuleRepo) {
        this.endpointHeaderRuleRepo = endpointHeaderRuleRepo;
    }

    @PostConstruct
    public void init() {
        log.info("🔧 EndpointHeaderRuleService @PostConstruct: Loading endpoint header rules...");
        loadEndpointHeaderRules().block(); // Load synchronously during bean initialization
    }

    // Load endpoint header rules at startup after loading endpoints and allowed headers
    public Mono<Void> loadEndpointHeaderRules() {
        return endpointHeaderRuleRepo.loadAllEndpointHeaderRules()
           .collectList()
           .doOnNext(list -> {
               endpointHeaderRule = Map.copyOf(
                   list.stream().collect(Collectors
                       .toMap(
                           item -> item.getEndpoint_id().toString(),
                           item -> {
                                List<EndpointHeaderRuleEntity> endpointHeaderRuleList = new ArrayList<>();
                                endpointHeaderRuleList.add(item);
                                return endpointHeaderRuleList;
                           },
                           (existing, replacement) -> {
                                existing.addAll(replacement);
                                return existing;
                           }
                       )
                   )
               );
           })
           .then();
    }
}
