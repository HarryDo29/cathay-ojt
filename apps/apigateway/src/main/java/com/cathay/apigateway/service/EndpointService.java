package com.cathay.apigateway.service;

import com.cathay.apigateway.core.routing.MatchResult;
import com.cathay.apigateway.core.routing.PathTrie;
import com.cathay.apigateway.entity.EndpointEntity;
import com.cathay.apigateway.interfaces.IEndpointRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class EndpointService {
    private final IEndpointRepository endpointRepo;
    private final PathTrie pathTrie;

    public EndpointService(IEndpointRepository endpointRepo, PathTrie pathTrie) {
        this.endpointRepo = endpointRepo;
        this.pathTrie = pathTrie;
    }

    @PostConstruct
    public void init() {
        log.info("[Endpoint] ▶️ Registering API endpoints into path trie...");
        loadEndpoints().block();
    }

    public Mono<Void> loadEndpoints() {
        return endpointRepo.getAllEndpoints()
                .collectList()
                .doOnNext(endpoints -> {
                    pathTrie.clear();
                    for (EndpointEntity ep : endpoints) {
                        pathTrie.insertTrie(ep);
                    }
                    log.info("[Gateway] ✅ Endpoint registry ready — {} endpoints registered", endpoints.size());
                })
                .then();
    }

    public MatchResult getEndpoint(String path, String method) {
        return pathTrie.matchTrie(path, method);
    }
}
