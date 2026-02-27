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
        log.info("[Gateway] ▶️ Loading rate limit configurations...");
        loadRateLimits().block();
        log.info("[Gateway] ✅ Rate limiter ready — {} rate limit rules configured", rateLimitList.size());
    }

    public Mono<Void> loadRateLimits() {
        return rateLimitRepository.getAllRateLimits()
                .collectList()
                .doOnNext(rateLimit -> rateLimitList = List.copyOf(rateLimit))
                .then();
    }

}
