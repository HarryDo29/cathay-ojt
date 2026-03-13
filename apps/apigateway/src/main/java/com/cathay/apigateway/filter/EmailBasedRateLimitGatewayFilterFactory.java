package com.cathay.apigateway.filter;

import com.cathay.apigateway.entity.EndpointsEntity;
import com.cathay.apigateway.entity.RateLimitEntity;
import com.cathay.apigateway.enums.KeyType;
import com.cathay.apigateway.enums.RateLimitType;
import com.cathay.apigateway.model.SlideWindowRule;
import com.cathay.apigateway.model.SlidingWindowState;
import com.cathay.apigateway.service.EndpointRegisterService;
import com.cathay.apigateway.service.RateLimitService;
import com.cathay.apigateway.util.ErrorHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
public class EmailBasedRateLimitGatewayFilterFactory
        extends AbstractGatewayFilterFactory<EmailBasedRateLimitGatewayFilterFactory.Config> {

    private final EndpointRegisterService endpointRegisterService;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;
    private final Cache<String, SlidingWindowState> emailRateLimitCache;
    private final ErrorHandler errorHandler;

    public EmailBasedRateLimitGatewayFilterFactory(EndpointRegisterService endpointRegisterService,
                                                   RateLimitService rateLimitService,
                                                   ObjectMapper objectMapper,
                                                   Cache<String, SlidingWindowState> emailRateLimitCache,
                                                   ErrorHandler errorHandler) {
        super(Config.class);
        this.endpointRegisterService = endpointRegisterService;
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
        this.emailRateLimitCache = emailRateLimitCache;
        this.errorHandler = errorHandler;
    }

    @Value("${internal.api.key}")
    private String internalApiKey;

    public boolean tryAccess(String email, SlideWindowRule rule) {
        String cacheKey = buildCacheKey(email, rule);
        SlidingWindowState state = emailRateLimitCache.get(cacheKey, k -> new SlidingWindowState(
                rule.getLimit(),
                Duration.ofSeconds(rule.getWindow())
        ));
        return state.tryConsume();
    }

    private static String buildCacheKey(String accountId, SlideWindowRule rule) {
        return accountId + "|" + "auth-service" + "|" + String.join(",", rule.getMethods());
    }


    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String uri = request.getURI().getPath();
            String method = request.getMethod().toString();

            SlideWindowRule rule = this.getEmailRateLimitEntity(); // get email-based rate limit rule from cached list
            if (rule == null) {
                log.info("No email-based rate limit rule found, skipping rate limit for URI: {}", uri);
                return chain.filter(exchange);
            }

            EndpointsEntity endpoint = endpointRegisterService.getEndpoint(uri, method).getEntity();
            if (endpoint.isPublic() && !(uri.matches(rule.getPath_regex())
                    && rule.getMethods().contains(method))) {
                log.info("Skipping email-based rate limit for public endpoint URI: {}", uri);
                return chain.filter(exchange);
            }

            return this.applyEmailRateLimit(exchange, chain, rule);
        };
    }

    // get email-based rate limit rule from cached list, return null if not found
    private SlideWindowRule getEmailRateLimitEntity() {
        RateLimitEntity rate_limit = rateLimitService.getRateLimitList().stream()
                .filter(rate -> rate.getKeyType() == KeyType.EMAIL
                        && rate.getType() == RateLimitType.SLIDING_WINDOW)
                .findFirst()
                .orElse(null);
        if (rate_limit == null) {
            return null;
        }
        return SlideWindowRule.fromJson(rate_limit.getRule());
    }

    private Mono<Void> applyEmailRateLimit(ServerWebExchange exchange,
                                           GatewayFilterChain chain,
                                           SlideWindowRule rule) {
        return ServerWebExchangeUtils.cacheRequestBody(exchange, serverHttpRequest -> {
            ServerRequest cacheRequest = ServerRequest.create(
                exchange.mutate().request(serverHttpRequest).build(),
                HandlerStrategies.withDefaults().messageReaders()
            );

            return cacheRequest.bodyToMono(String.class)
                .flatMap(bodyString -> {
                    try {
                        JsonNode rootNode = objectMapper.readTree(bodyString);
                        JsonNode emailNode = rootNode.path("email");

                        if (!emailNode.isMissingNode() && !emailNode.isNull()) {
                        String email = emailNode.asText();
                        exchange.getAttributes().put("USER_EMAIL", email);

                        if (this.tryAccess(email, rule)) {
                        return chain.filter(exchange.mutate().request(serverHttpRequest).build());
                        }
                        return errorHandler.writeError(exchange,
                                new IllegalArgumentException("Too many requests for email: " + email),
                                HttpStatus.TOO_MANY_REQUESTS
                        );
                    }

                    return errorHandler.writeError(exchange,
                            new IllegalArgumentException("Email not found in request body"),
                            HttpStatus.BAD_REQUEST);
                    } catch (Exception e) {
                        log.error("Error reading body JSON: {}", e.getMessage());
                        return Mono.error(new IllegalArgumentException("Invalid JSON body"));
                    }
                });
            });
    }

    public static class Config {}
}
