package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.CorsConfig;
import com.cathay.apigateway.entity.AllowedHeaderEntity;
import com.cathay.apigateway.interfaces.IAllowedHeaderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class YmlAllowedHeaderRepository implements IAllowedHeaderRepository {
    private final CorsConfig corsConfig;

    @Override
    public Flux<AllowedHeaderEntity> loadAllowedHeaders() {
        List<CorsConfig.AllowedHeader> allowedHeaderList = corsConfig.getAllowedHeaders();
        return Flux.fromIterable(allowedHeaderList)
                .map(allowedHeader -> {;
                    AllowedHeaderEntity entity = new AllowedHeaderEntity();
                    entity.setId(allowedHeader.getId());
                    entity.setHeader(allowedHeader.getHeader());
                    entity.setEnabled(Boolean.parseBoolean(allowedHeader.getEnabled()));
                    return entity;
                });
    }
}
