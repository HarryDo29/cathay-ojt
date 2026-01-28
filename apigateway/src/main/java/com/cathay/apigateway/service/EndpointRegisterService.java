package com.cathay.apigateway.service;
import com.cathay.apigateway.entity.EndpointsEntity;
import com.cathay.apigateway.interfaces.IEndpointServiceRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.Set;

@Service
public class EndpointRegisterService {
    private final IEndpointServiceRepository endpointRepo;
    private volatile Set<EndpointsEntity> publicEndpoints = Set.of();

    public EndpointRegisterService(IEndpointServiceRepository endpointRepo) {
        this.endpointRepo = endpointRepo;
    }

    public Mono<Void> loadEndpoints() {
        return endpointRepo.getAllEndpoints()
                .collectList()
                .doOnNext(endpoints -> publicEndpoints = Set.copyOf(endpoints))
                .then();
    }

    public boolean isPublic(String path) {
        return publicEndpoints.stream().findFirst().get().isPublic();
    }

    public EndpointsEntity isEnabled(String path) {
        EndpointsEntity endpoint = publicEndpoints.stream()
                .filter(ep -> ep.getPath().equals(path))
                .findFirst()
                .orElse(null);
        return endpoint;
    }
}
