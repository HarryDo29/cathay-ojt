package com.cathay.apigateway.service;

import com.cathay.apigateway.entity.AllowedHeaderEntity;
import com.cathay.apigateway.entity.AllowedOriginEntity;
import com.cathay.apigateway.interfaces.IAllowedHeaderRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AllowedHeaderService {
    private final IAllowedHeaderRepository allowedHeaderRepository;

    @Getter
    private volatile List<AllowedHeaderEntity> allowedHeaderList =List.of();

    public AllowedHeaderService(IAllowedHeaderRepository allowedHeaderRepository) {
        this.allowedHeaderRepository = allowedHeaderRepository;
    }

    @PostConstruct
    public void init() {
        log.info("🔧 AllowedHeaderService @PostConstruct: Loading allowed headers...");
        loadAllowedHeaders().block(); // Load synchronously during bean initialization
    }

    public Mono<Void> loadAllowedHeaders() {
        return allowedHeaderRepository.loadAllowedHeaders()
                .collectList()
                .doOnNext(headers -> {
                    allowedHeaderList = new ArrayList<>(headers
                            .stream().filter(AllowedHeaderEntity::getEnabled).toList()
                    );
                })
                .then();
    }
}
