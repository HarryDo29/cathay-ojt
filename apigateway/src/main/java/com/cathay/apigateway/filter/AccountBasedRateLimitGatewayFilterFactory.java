package com.cathay.apigateway.filter;

import com.cathay.apigateway.entity.RateLimitEntity;
import com.cathay.apigateway.enums.KeyType;
import com.cathay.apigateway.service.RateLimitService;
import com.cathay.apigateway.util.ErrorHandler;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class AccountBasedRateLimitGatewayFilterFactory
    extends AbstractGatewayFilterFactory<AccountBasedRateLimitGatewayFilterFactory.Config> {

    private static final String DEFAULT_TOKEN_COST = "1";

    private final RedisScript<Long> accountRateLimitLuaScript;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final RateLimitService rateLimitService;
    private final ErrorHandler errorHandler;

    @Override
    public GatewayFilter apply(AccountBasedRateLimitGatewayFilterFactory.Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String accountId = request.getHeaders().getFirst("X-User-Id");

            if (accountId == null || accountId.isEmpty()) {
                log.warn("Missing X-User-Id header for request: {}", request.getURI());
                return errorHandler.writeError(exchange,
                        new IllegalArgumentException("Missing X-User-Id header"), HttpStatus.FORBIDDEN);
            }

            RateLimitEntity rateLimitEntity = findAccountRateLimitConfig();
            if  (rateLimitEntity == null) {
                log.warn("No account-based rate limit configuration found, allowing request");
                return chain.filter(exchange);
            }

            return executeRateLimit(accountId, rateLimitEntity)
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
    private RateLimitEntity findAccountRateLimitConfig() {
        return rateLimitService.getRateLimitList()
                .stream()
                .filter(r -> r.getKeyType() == KeyType.ACCOUNT_ID
                        && Boolean.TRUE.equals(r.getEnabled()))
                .findFirst()
                .orElse(null);
    }

    // Execute the Lua script for rate limiting and return whether the request is allowed
    private Mono<Boolean> executeRateLimit(String accountId, RateLimitEntity config) {
        List<String> keys = Collections.singletonList(accountId);
        List<String> args = List.of(
                config.getBurstCapacity().toString(),
                config.getReplenishRate().toString(),
                String.valueOf(Instant.now().toEpochMilli()),
                DEFAULT_TOKEN_COST
        );

        return reactiveRedisTemplate.execute(accountRateLimitLuaScript, keys, args)
                .next()
                .map(result -> result == 1L);
    }

    public static class Config {}
}
