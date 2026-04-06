package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.CorsConfig;
import com.cathay.apigateway.entity.AllowedOriginEntity;
import com.cathay.apigateway.interfaces.IAllowedOriginRepository;
import com.cathay.apigateway.r2dbcRepository.R2dbcAllowedOriginRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AllowedOriginRepository implements IAllowedOriginRepository {
    private final CorsConfig corsConfig;
    private final R2dbcAllowedOriginRepository allowedOriginRepo;

    @Override
    public Flux<AllowedOriginEntity> loadAllAllowedOrigins() {
        List<CorsConfig.AllowedOrigin> ymlAllowedOriginList = corsConfig.getAllowedOrigins();
        return allowedOriginRepo.findAll()
                .collectList()
                .flatMapMany(allowedOriginList ->{
                    if (!allowedOriginList.isEmpty()){
                        return Flux.fromIterable(allowedOriginList);
                    }
                    return Flux.empty();
                })
                .onErrorResume(error -> {
                    // Log the error if needed
                    System.err.println("Error loading allowed origins from DB: " + error.getMessage());
                    // Fallback to YML configuration
                    return Flux.fromIterable(ymlAllowedOriginList)
                            .map(allowedOrigin -> {;
                                AllowedOriginEntity entity = new AllowedOriginEntity();
                                entity.setId(allowedOrigin.getId());
                                entity.setOrigin(allowedOrigin.getOrigin());
                                entity.setEnabled(Boolean.parseBoolean(allowedOrigin.getEnabled()));
                                return entity;
                            });
                });
    }
}
