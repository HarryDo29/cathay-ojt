package com.cathay.apigateway.filter;

import com.cathay.apigateway.entity.RateLimitEntity;
import com.cathay.apigateway.enums.KeyType;
import com.cathay.apigateway.enums.RateLimitType;
import com.cathay.apigateway.model.SlideWindowRule;
import com.cathay.apigateway.model.TokenBucketRule;
import com.cathay.apigateway.service.RateLimitService;
import com.cathay.apigateway.util.ErrorHandler;
import com.cathay.apigateway.util.RequestUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class IPBasedRateLimitGlobalFilter implements GlobalFilter, Ordered {
    private static final int ORDER = -100;

    private final RateLimitService rateLimitService;
    private final ErrorHandler errorHandler;

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build();

    private Bucket createNewBucket(TokenBucketRule rule) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(rule.getBurst_capacity())
                .refillGreedy(rule.getReplenish_rate(), Duration.ofMinutes(rule.getTtl()))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public boolean tryAccess(String key, TokenBucketRule rule) {
        Bucket bucket = cache.get(key, k -> createNewBucket(rule));
        return bucket.tryConsume(1);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String ip = RequestUtil.getClientIp(request);

        if (ip.equals("unknown") || ip.isEmpty()) {
            log.warn("Unable to determine client IP for request: {}", request.getURI());
            return errorHandler.writeError(exchange,
                    new NotFoundException("Missing IP Address"), HttpStatus.FORBIDDEN);
        }

        TokenBucketRule rule = findIpRateLimitConfig();
        if (rule == null) {
            log.warn("No IP-based rate limit configuration found, allowing request");
            return chain.filter(exchange);
        }

        if (this.tryAccess(ip, rule)){
            log.info("IP {} allowed by rate limit rule", ip);
            return chain.filter(exchange);
        }
        log.warn("IP {} blocked by rate limit rule", ip);
        return errorHandler.writeError(exchange,
                new NotFoundException("Rate limit exceeded"), HttpStatus.TOO_MANY_REQUESTS);
    }

    // Get rate limit rule for IP-based key type
    private TokenBucketRule findIpRateLimitConfig() {
         RateLimitEntity rate = rateLimitService.getRateLimitList()
                .stream()
                .filter(r -> r.getKeyType() == KeyType.IP && r.getType() == RateLimitType.TOKEN_BUCKET)
                .findFirst()
                .orElse(null);
         return rate == null ? null : TokenBucketRule.fromJson(rate.getRule());
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
