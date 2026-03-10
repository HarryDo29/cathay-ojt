package com.cathay.apigateway.filter;

import com.cathay.apigateway.entity.RateLimitEntity;
import com.cathay.apigateway.enums.KeyType;
import com.cathay.apigateway.enums.RateLimitType;
import com.cathay.apigateway.model.SlideWindowRule;
import com.cathay.apigateway.service.RateLimitService;
import com.cathay.apigateway.util.ErrorHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class EmailBasedRateLimitGatewayFilter
        extends AbstractGatewayFilterFactory<EmailBasedRateLimitGatewayFilter.Config> {
    private final ObjectMapper objectMapper;

    private final RedisScript<Long> emailRateLimitLuaScript;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final RateLimitService rateLimitService;
    private final ErrorHandler errorHandler;

    public EmailBasedRateLimitGatewayFilter(RedisScript<Long> emailRateLimitLuaScript,
                                            ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
                                            RateLimitService rateLimitService,
                                            ObjectMapper objectMapper,
                                            ErrorHandler errorHandler) {
        super(Config.class);
        this.emailRateLimitLuaScript = emailRateLimitLuaScript;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
        this.errorHandler = errorHandler;
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

            String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
            return getEmailFromRequestBody(exchange)
                    .flatMap(email -> {
                        if (email == null || email.isBlank()) {
                            log.warn("Missing email in request body for URI: {}", uri);
                            return errorHandler.writeError(exchange,
                                    new IllegalArgumentException("Missing email in request body"),
                                    HttpStatus.BAD_REQUEST);
                        }

                        return executeRateLimit(email, requestId, rule)
                            .flatMap(allowed -> {
                                if (allowed) {
                                    log.info("Email allowed by rate limit for URI: {}", uri);
                                    return chain.filter(exchange);
                                }
                                log.warn("Email blocked by rate limit for URI: {}", uri);
                                return errorHandler.writeError(exchange,
                                    new RuntimeException("Too many requests"),
                                    HttpStatus.TOO_MANY_REQUESTS);
                            });
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        log.warn("Missing email in request body for URI: {}", uri);
                        return errorHandler.writeError(exchange,
                                new IllegalArgumentException("Missing email in request body"),
                                HttpStatus.BAD_REQUEST);
                    }))
                    .onErrorResume(IllegalArgumentException.class, ex ->
                            errorHandler.writeError(exchange, ex, HttpStatus.BAD_REQUEST))
                    .onErrorResume(ex -> {
                        log.error("Rate limit check failed (fail-open): {}", ex.getMessage(), ex);
                        return chain.filter(exchange);
                    });
        };
    }

    // Execute the Lua script for rate limiting and return whether the request is allowed
    private Mono<Boolean> executeRateLimit(String email, String request_id, SlideWindowRule config) {
        // use accountId as the key for rate limiting
        List<String> keys = Collections.singletonList(email);
        List<String> args = List.of(
                config.getLimit().toString(),
                config.getWindow().toString(),
                String.valueOf(Instant.now().toEpochMilli()),
                request_id
        );

        return reactiveRedisTemplate.execute(emailRateLimitLuaScript, keys, args)
                .next()
                .map(result -> result == 1L);
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
