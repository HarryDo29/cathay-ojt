package com.cathay.apigateway.filter;

import com.cathay.apigateway.entity.RateLimitEntity;
import com.cathay.apigateway.enums.KeyType;
import com.cathay.apigateway.service.RateLimitService;
import com.cathay.apigateway.util.ErrorHandler;
import com.cathay.apigateway.util.RequestUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class IPBasedRateLimitGlobalFilter implements GlobalFilter, Ordered {
    private static final int ORDER = -100;
    private static final String DEFAULT_TOKEN_COST = "1";

    private final RedisScript<Long> ipRateLimitLuaScript;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final RateLimitService rateLimitService;
    private final ErrorHandler errorHandler;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String ip = RequestUtil.getClientIp(request);

        if (ip.equals("unknown") || ip.isEmpty()) {
            log.warn("Unable to determine client IP for request: {}", request.getURI());
            return errorHandler.writeError(exchange,
                    new NotFoundException("Missing IP Address"), HttpStatus.FORBIDDEN);
        }

        RateLimitEntity config = findIpRateLimitConfig();
        if (config == null) {
            log.warn("No IP-based rate limit configuration found, allowing request");
            return chain.filter(exchange);
        }

        return executeRateLimit(ip, config)
                .flatMap(allowed -> {
                    if (allowed) {
                        return chain.filter(exchange);
                    }
                    log.warn("Rate limit exceeded for IP: {}", ip);
                    return errorHandler.writeError(exchange,
                            new NotFoundException("Rate limit exceeded"), HttpStatus.TOO_MANY_REQUESTS);
                })
                .onErrorResume(ex -> {
                    log.error("Rate limit check failed (fail-open): {}", ex.toString());
                    return chain.filter(exchange);
                });
    }

    // Get rate limit rule for IP-based key type
    private RateLimitEntity findIpRateLimitConfig() {
        return rateLimitService.getRateLimitList()
                .stream()
                .filter(r -> r.getKeyType() == KeyType.IP)
                .findFirst()
                .orElse(null);
    }

    // Execute the Lua script for rate limiting and return whether the request is allowed
    private Mono<Boolean> executeRateLimit(String ip, RateLimitEntity config) {
        List<String> keys = Collections.singletonList(ip);
        List<String> args = List.of(
                config.getBurstCapacity().toString(),
                config.getReplenishRate().toString(),
                String.valueOf(Instant.now().toEpochMilli()),
                DEFAULT_TOKEN_COST,
                String.valueOf(config.getTtl())
        );
        log.info("Executing IP-based rate limit for request: {}", ip);
        return reactiveRedisTemplate.execute(ipRateLimitLuaScript, keys, args)
                .next()
                .map(result -> result == 1L);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
