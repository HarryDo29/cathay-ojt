package com.cathay.apigateway.repository;

import com.cathay.apigateway.data.config.CorsConfig.AllowedHeader;
import com.cathay.apigateway.data.config.CorsConfig;
import com.cathay.apigateway.entity.AllowedHeaderEntity;
import com.cathay.apigateway.interfaces.IAllowedHeaderRepository;
import com.cathay.apigateway.r2dbcRepository.R2dbcAllowedHeaderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AllowedHeaderRepository implements IAllowedHeaderRepository {
    private final CorsConfig corsConfig;
    private final R2dbcAllowedHeaderRepository allowedHeaderRepo;

    @Override
    public Flux<AllowedHeaderEntity> loadAllowedHeaders() {
        List<AllowedHeader> ymlAllowedHeaderList = corsConfig.getAllowedHeaders();
        return allowedHeaderRepo.findAll()
                .collectList()
                .flatMapMany(allowedHeaderList -> {
                    if (!allowedHeaderList.isEmpty()) {
                        return Flux.fromIterable(allowedHeaderList);
                    }
                    return Flux.empty();
                })
                .onErrorResume(error -> {
                            // Log the error if needed
                            System.err.println("Error loading allowed headers from DB: " + error.getMessage());
                            // Fallback to YML configuration
                            return Flux.fromIterable(ymlAllowedHeaderList)
                                    .map(allowedHeader -> {
                                        AllowedHeaderEntity entity = new AllowedHeaderEntity();
                                        entity.setId(allowedHeader.getId());
                                        entity.setHeader(allowedHeader.getHeader());
                                        entity.setEnabled(Boolean.parseBoolean(allowedHeader.getEnabled()));
                                        return entity;
                                    });
                        }
                );
    }
}
