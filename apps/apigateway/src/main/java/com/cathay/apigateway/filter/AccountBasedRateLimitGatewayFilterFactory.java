package com.cathay.apigateway.filter;

import com.cathay.apigateway.model.SlideWindowRule;
import com.cathay.apigateway.service.RateLimitService;
import com.cathay.apigateway.util.ErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class AccountBasedRateLimitGatewayFilterFactory
    extends AbstractGatewayFilterFactory<AccountBasedRateLimitGatewayFilterFactory.Config> {

    private final RedisScript<Long> accountRateLimitLuaScript;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final RateLimitService rateLimitService;
    private final ErrorHandler errorHandler;

    public AccountBasedRateLimitGatewayFilterFactory(RedisScript<Long> accountRateLimitLuaScript,
                                                      ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
                                                      RateLimitService rateLimitService,
                                                      ErrorHandler errorHandler) {
        super(Config.class);
        this.accountRateLimitLuaScript = accountRateLimitLuaScript;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.rateLimitService = rateLimitService;
        this.errorHandler = errorHandler;
    }

    @Override
    public GatewayFilter apply(AccountBasedRateLimitGatewayFilterFactory.Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String isPublicEndpoint = request.getHeaders().getFirst("Public-Endpoint");
            if ("true".equalsIgnoreCase(isPublicEndpoint)) {
                log.info("Public endpoint detected, skipping account-based rate limit.");
                return chain.filter(exchange);
            }
            String accountId = request.getHeaders().getFirst("X-User-Id");

            if (accountId == null || accountId.isEmpty()) {
                log.warn("Missing X-User-Id header for request: {}", request.getURI());
                return errorHandler.writeError(exchange,
                        new IllegalArgumentException("Missing X-User-Id header"), HttpStatus.FORBIDDEN);
            }
            String uri = request.getURI().getPath();
            String method = request.getMethod().name();
            SlideWindowRule rule = findAccountRateLimitConfig(uri, method);
            if  (rule == null) {
                log.warn("No account-based rate limit configuration found, allowing request");
                return chain.filter(exchange);
            }
            String request_id = request.getHeaders().getFirst("X-Request-Id");
            return executeRateLimit(accountId, request_id, rule)
                    .flatMap(allowed -> {
                        if (allowed) {
                            return chain.filter(exchange);
                        } else {
                            log.warn("Account {} has exceeded rate limit", accountId);
                            return errorHandler.writeError(exchange,
                                    new IllegalStateException("Rate limit exceeded"), HttpStatus.TOO_MANY_REQUESTS);
                        }
                    })
                    .onErrorResume(ex -> {
                        log.error("Rate limit check failed (fail-open): {}", ex.getMessage());
                        return chain.filter(exchange);
                    });
        };
    }

    // Get rate limit rule for IP-based key type
    private SlideWindowRule findAccountRateLimitConfig(String uri, String method) {
        return rateLimitService.getSlideWindowRuleList()
                .stream()
                .filter(rule ->
                    uri.matches(rule.getPath_regex()) && List.of(rule.getMethods()).contains(method)
                )
                .findFirst()
                .orElse(null);
    }

    // Execute the Lua script for rate limiting and return whether the request is allowed
    private Mono<Boolean> executeRateLimit(String accountId, String request_id, SlideWindowRule config) {
        List<String> keys = Collections.singletonList(accountId + ":" + config.getPriority()); // use accountId as the key for rate limiting
        List<String> args = List.of(
                config.getLimit().toString(),
                config.getWindow().toString(),
                String.valueOf(Instant.now().toEpochMilli()),
                request_id
        );

        return reactiveRedisTemplate.execute(accountRateLimitLuaScript, keys, args)
                .next()
                .map(result -> result == 1L);
    }

    public static class Config {}
}
