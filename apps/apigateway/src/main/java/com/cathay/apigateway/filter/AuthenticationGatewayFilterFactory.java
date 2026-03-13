package com.cathay.apigateway.filter;

import com.cathay.apigateway.entity.EndpointsEntity;
import com.cathay.apigateway.service.EndpointRegisterService;
import com.cathay.apigateway.util.ErrorHandler;
import com.cathay.apigateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class AuthenticationGatewayFilterFactory extends
        AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    private final EndpointRegisterService endpointRegisterService;
    private final JwtUtil jwtUtil;
    private final ErrorHandler errorHandler;

    public AuthenticationGatewayFilterFactory(EndpointRegisterService endpointRegisterService,
                                              JwtUtil jwtUtil, ErrorHandler errorHandler) {
        super(Config.class);
        this.endpointRegisterService = endpointRegisterService;
        this.jwtUtil = jwtUtil;
        this.errorHandler = errorHandler;
    }

    @Value("${internal.api.key}")
    private String internalApiKey;

    @Override
    public @NonNull GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            val path = exchange.getRequest().getURI().getPath();
            val method = exchange.getRequest().getMethod();
            log.info("\uD83D\uDD10 Authenticating request for path: {}", path);
            EndpointsEntity endpoint = endpointRegisterService.getEndpoint(path, method.toString()).getEntity();
            if (endpoint.isPublic()) {
                log.info("\uD83D\uDD10 Skipping authentication for public endpoint: {}", path);
                ServerHttpRequest req = exchange.getRequest()
                        .mutate()
                        .header("X-Internal-API-Key", internalApiKey)  // ← Thêm key cho public endpoints
                        .header("Public-Endpoint", "true")  // Optional: đánh dấu đây là public endpoint
                        .build();
                return chain.filter(exchange.mutate().request(req).build());
            }

            List<String> authHeader = exchange.getRequest()
                    .getHeaders()
                    .get(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || authHeader.isEmpty()) {
                return errorHandler.writeError(exchange,
                        new RuntimeException("Missing Authorization header"),
                        HttpStatus.UNAUTHORIZED);
            }

            // validate authorization (`Bearer ...` ?) ()
            if (!authHeader.getFirst().startsWith("Bearer ")) {
                return errorHandler.writeError(exchange,
                        new RuntimeException("Missing Authorization header"),
                        HttpStatus.UNAUTHORIZED);
            }

            // Extract and verify JWT token
            Claims claim;
            try {
                claim = jwtUtil.extractToken(authHeader.getFirst().substring(7));
            } catch (Exception e) {
                return errorHandler.writeError(exchange, e, HttpStatus.UNAUTHORIZED);
            }

            // check expiration
            val expiration = claim.getExpiration();
            if (expiration.before(new Date())) {
                return errorHandler.writeError(exchange,
                        new RuntimeException("Access token has expired"),
                        HttpStatus.UNAUTHORIZED);
            }

            // take account in4 from token
            String account_id = claim.getSubject();
            String email = jwtUtil.extractClaim(
                    claim, claims -> claims.get("email", String.class));
            String role = jwtUtil.extractClaim(
                    claim, claims -> claims.get("role", String.class));

            // remove sensitive info from claim (if any) before forward to downstream service
            String existingApiKey = exchange.getRequest().getHeaders().getFirst("X-Internal-API-Key");
            if (existingApiKey != null && !existingApiKey.isEmpty()) {
                // Header exists and is not empty
                exchange.getRequest().getHeaders().remove("X-Internal-API-Key");
            }

            // Create new request with header contain account in4 and internal API key
            ServerHttpRequest req = exchange.getRequest()
                    .mutate()
                    .header("X-User-Id", account_id != null ? account_id : "")
                    .header("X-User-Email", email != null ? email : "")
                    .header("X-User-Role", role != null ? role : "")
                    .header("X-Internal-API-Key", internalApiKey)  // Thêm internal API key
                    .build();
            return chain.filter(exchange.mutate().request(req).build());
        };
    }

    public static class Config {
    }
}
