package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.CorsConfig;
import com.cathay.apigateway.entity.AllowedOriginEntity;
import com.cathay.apigateway.interfaces.IAllowedOriginRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class YmlAllowedOriginRepository implements IAllowedOriginRepository {
    private final CorsConfig corsConfig;

    @Override
    public Flux<AllowedOriginEntity> loadAllAllowedOrigins() {
        List<CorsConfig.AllowedOrigin> allowedOriginList = corsConfig.getAllowedOrigins();
        return Flux.fromIterable(allowedOriginList)
                .map(allowedOrigin -> {;
                    AllowedOriginEntity entity = new AllowedOriginEntity();
                    entity.setId(allowedOrigin.getId());
                    entity.setOrigin(allowedOrigin.getOrigin());
                    entity.setEnabled(Boolean.parseBoolean(allowedOrigin.getEnabled()));
                    return entity;
                });
    }
}
