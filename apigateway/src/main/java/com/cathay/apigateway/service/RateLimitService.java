package com.cathay.apigateway.service;

import com.cathay.apigateway.entity.RateLimitEntity;
import com.cathay.apigateway.interfaces.IRateLimitRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
public class RateLimitService {
    @Getter
    public volatile List<RateLimitEntity> rateLimitList = List.of();

    private final IRateLimitRepository rateLimitRepository;

    public RateLimitService(IRateLimitRepository rateLimitRepository) {
        this.rateLimitRepository = rateLimitRepository;
    }

    @PostConstruct
    public void init() {
        log.info("🔧 RateLimitService @PostConstruct: Loading rate limit rules...");
        loadRateLimits().block(); // Load synchronously during bean initialization
        log.info("✅ RateLimitService initialized with " + rateLimitList.size() + " rate limit rules");
    }

    public Mono<Void> loadRateLimits() {
        return rateLimitRepository.getAllRateLimits()
                .collectList()
                .doOnNext(rateLimit -> {
                    rateLimitList = List.copyOf(rateLimit);
                    log.info("✅ Loaded " + rateLimitList.size() + " rate limit rules");
                })
                .then();
    }

}
