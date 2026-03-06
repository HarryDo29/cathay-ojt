package com.cathay.apigateway.service;

import com.cathay.apigateway.entity.AllowedOriginEntity;
import com.cathay.apigateway.interfaces.IAllowedOriginRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AllowedOriginService {
    private final IAllowedOriginRepository allowedOriginRepository;

    @Getter
    private List<AllowedOriginEntity> allowedOriginList;

    public AllowedOriginService(IAllowedOriginRepository allowedOriginRepository) {
        this.allowedOriginRepository = allowedOriginRepository;
    }

    @PostConstruct
    public void init() {
        log.info("[Gateway] ▶️ Loading CORS allowed origins...");
        loadAllowedOrigins().block();
        log.info("[Gateway] ✅ CORS origins ready — {} allowed origins active", allowedOriginList.size());
    }

    public Mono<Void> loadAllowedOrigins() {
        return allowedOriginRepository.loadAllAllowedOrigins()
                .collectList()
                .doOnNext(origins -> {
                    allowedOriginList = new ArrayList<>(origins
                            .stream().filter(AllowedOriginEntity::isEnabled).toList()
                    );
                })
                .then();
    }


}
