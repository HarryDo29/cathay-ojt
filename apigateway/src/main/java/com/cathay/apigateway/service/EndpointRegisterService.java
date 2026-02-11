package com.cathay.apigateway.service;
import com.cathay.apigateway.core.routing.MatchResult;
import com.cathay.apigateway.core.routing.PathTrie;
import com.cathay.apigateway.entity.EndpointsEntity;
import com.cathay.apigateway.interfaces.IEndpointServiceRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class EndpointRegisterService {
    private final IEndpointServiceRepository endpointRepo;
    private final PathTrie pathTrie;

    public EndpointRegisterService(IEndpointServiceRepository endpointRepo, PathTrie pathTrie) {
        this.endpointRepo = endpointRepo;
        this.pathTrie = pathTrie;
    }

    @PostConstruct
    public void init() {
        log.info("🔧 EndpointRegisterService @PostConstruct: Loading endpoints...");
        loadEndpoints().block(); // Load synchronously during bean initialization
    }

    public Mono<Void> loadEndpoints() {
        return endpointRepo.getAllEndpoints()
                .collectList()
                .doOnNext(endpoints -> {
                    for( EndpointsEntity ep : endpoints) {
                        pathTrie.insertTrie(ep);
                    }
                })
                .then();
    }

    public MatchResult getEndpoint(String path, String method) {
        return pathTrie.matchTrie(path, method);
    }
}
