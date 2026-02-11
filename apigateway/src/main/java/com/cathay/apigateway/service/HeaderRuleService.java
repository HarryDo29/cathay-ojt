package com.cathay.apigateway.service;

import com.cathay.apigateway.entity.HeaderRulesEntity;
import com.cathay.apigateway.interfaces.IHeaderRuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HeaderRuleService {
    @Getter
    private volatile Map<String, HeaderRulesEntity> headers = Map.of(); // Initialize with empty set

    private final IHeaderRuleRepository allowedHeaderRepo;

    public HeaderRuleService(IHeaderRuleRepository allowedHeaderRepo) {
        this.allowedHeaderRepo = allowedHeaderRepo;
    }

    // Load allowed headers at startup after loading endpoints
    @PostConstruct
    public void init() {
        log.info("🔧 HeaderRuleService @PostConstruct: Loading allowed headers...");
        loadAllowedHeaders().block(); // Load synchronously during bean initialization
    }

    public Mono<Void> loadAllowedHeaders(){
        return allowedHeaderRepo.getAllAllowedHeaders()
                .collectList()
                .doOnNext(allowedHeaderList ->
                        headers = Map.copyOf(
                                allowedHeaderList.stream().collect(
                                        Collectors.toMap(
                                                header -> header.getId().toString(),
                                                header -> header,
                                                (existing, replacement) -> existing
                                        )
                                )
                        )
                )
                .then();
    }
}
