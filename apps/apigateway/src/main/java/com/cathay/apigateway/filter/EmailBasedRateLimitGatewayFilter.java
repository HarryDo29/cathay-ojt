package com.cathay.apigateway.filter;

import com.cathay.apigateway.entity.RateLimitEntity;
import com.cathay.apigateway.enums.KeyType;
import com.cathay.apigateway.enums.RateLimitType;
import com.cathay.apigateway.model.SlideWindowRule;
import com.cathay.apigateway.model.SlidingWindowState;
import com.cathay.apigateway.model.TokenBucketRule;
import com.cathay.apigateway.service.RateLimitService;
import com.cathay.apigateway.util.ErrorHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
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
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class EmailBasedRateLimitGatewayFilter
        extends AbstractGatewayFilterFactory<EmailBasedRateLimitGatewayFilter.Config> {
    private final ObjectMapper objectMapper;

    private final RateLimitService rateLimitService;
    private final ErrorHandler errorHandler;

    public EmailBasedRateLimitGatewayFilter(RateLimitService rateLimitService,
                                            ObjectMapper objectMapper,
                                            ErrorHandler errorHandler) {
        super(Config.class);
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
        this.errorHandler = errorHandler;
    }

    private final Cache<String, SlidingWindowState> cache = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    public boolean tryAccess(String email, SlideWindowRule rule) {
        String cacheKey = buildCacheKey(email, rule);
        SlidingWindowState state = cache.get(cacheKey, k -> new SlidingWindowState(
                rule.getLimit(),
                Duration.ofSeconds(rule.getWindow())
        ));
        return state.tryConsume();
    }

    private static String buildCacheKey(String accountId, SlideWindowRule rule) {
        return accountId + "|" + "auth-service" + "|" + String.join(",", rule.getMethods());
    }


    @Override
    public GatewayFilter apply(EmailBasedRateLimitGatewayFilter.Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String uri = request.getURI().getPath();
            SlideWindowRule rule = this.getEmailRateLimitEntity(); // get email-based rate limit rule from cached list
            if (rule == null) {
                log.info("No email-based rate limit rule found, skipping rate limit for URI: {}", uri);
                return chain.filter(exchange);
            }
            return getEmailFromRequestBody(exchange) // get email from request body
                    .flatMap(email -> {
                        if (email.isEmpty()) {
                            log.warn("No email found in request body for URI: {}", uri);
                            return errorHandler.writeError(exchange,
                                    new IllegalArgumentException("Missing email in request body"), HttpStatus.FORBIDDEN);
                        }
                        if (this.tryAccess(email, rule)) { // check rate limit
                            log.info("Email {} allowed by rate limit rule for URI: {}", email, uri);
                            return chain.filter(exchange);
                        }
                        log.warn("Email {} blocked by rate limit rule for URI: {}", email, uri);
                        return errorHandler.writeError(exchange,
                                new IllegalArgumentException("Too Many Requests"), HttpStatus.TOO_MANY_REQUESTS);
                    });
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

    // get email from request body, return null if not found
    private Mono<String> getEmailFromRequestBody(ServerWebExchange exchange) {
        // cache request body to avoid reading body multiple times
        return ServerWebExchangeUtils.cacheRequestBody(exchange, serverHttpRequest -> {
            // create server request from cached request body
            ServerRequest serverRequest = ServerRequest.create(
                    exchange.mutate().request(serverHttpRequest).build(),
                    HandlerStrategies.withDefaults().messageReaders()
            );
            // read body as string and parse jackson to get email
            return serverRequest.bodyToMono(String.class)
                    .flatMap(bodyString -> {
                        // parse jackson body to get email
                        try {
                            JsonNode rootNode = objectMapper.readTree(bodyString);
                            JsonNode emailNode = rootNode.path("email");
                            if (!emailNode.isMissingNode() && !emailNode.isNull()) {
                                String email = emailNode.asText();
                                // save email to exchange attributes to be used by other filters
                                exchange.getAttributes().put("USER_EMAIL", email);
                                // return email as mono to be used by other filters
                                return Mono.just(email);
                            }
                            return Mono.empty(); // no email found
                        } catch (Exception e) {
                            log.error("Error reading body JSON: {}", e.getMessage());
                            return Mono.error(new IllegalArgumentException("Invalid JSON body"));
                        }
                    });
        });
    }

    public static class Config {}
}
