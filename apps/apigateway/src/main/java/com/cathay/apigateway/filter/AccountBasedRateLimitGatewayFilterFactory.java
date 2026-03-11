package com.cathay.apigateway.filter;

import com.cathay.apigateway.model.SlideWindowRule;
import com.cathay.apigateway.model.SlidingWindowState;
import com.cathay.apigateway.service.RateLimitService;
import com.cathay.apigateway.util.ErrorHandler;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AccountBasedRateLimitGatewayFilterFactory
    extends AbstractGatewayFilterFactory<AccountBasedRateLimitGatewayFilterFactory.Config> {

    private final RateLimitService rateLimitService;
    private final ErrorHandler errorHandler;

    public AccountBasedRateLimitGatewayFilterFactory(RateLimitService rateLimitService,
                                                     ErrorHandler errorHandler) {
        super(Config.class);
        this.rateLimitService = rateLimitService;
        this.errorHandler = errorHandler;
    }

    private final Cache<String, SlidingWindowState> cache = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    public boolean tryAccess(String accountId, String uri, SlideWindowRule rule) {
        String cacheKey = buildCacheKey(accountId, uri, rule);
        SlidingWindowState state = cache.get(cacheKey, k -> new SlidingWindowState(
                rule.getLimit(),
                Duration.ofSeconds(rule.getWindow())
        ));
        return state.tryConsume();
    }

    private static String buildCacheKey(String accountId, String uri, SlideWindowRule rule) {
        String servicePart = Arrays.stream(uri.split("/"))
                .filter(r -> r.contains("service"))
                .findFirst()
                .orElse("unknown_service");
        return accountId + "|" + servicePart + "|" + String.join(",", rule.getMethods());
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
            if (rule == null) {
                log.debug("No account-based rate limit configuration for {} {}, allowing request", method, uri);
                return chain.filter(exchange);
            }

            if (tryAccess(accountId, uri, rule)) {
                log.debug("Account {} allowed by sliding window for {} {}", accountId, method, uri);
                return chain.filter(exchange);
            }
            log.warn("Account {} blocked by sliding window (limit {} per {}s) for {} {}", accountId, rule.getLimit(), rule.getWindow(), method, uri);
            return errorHandler.writeError(exchange,
                    new RuntimeException("Too many requests"), HttpStatus.TOO_MANY_REQUESTS);
        };
    }

    private SlideWindowRule findAccountRateLimitConfig(String uri, String method) {
        return rateLimitService.getSlideWindowRuleList()
                .stream()
                .filter(rule ->
                    uri.matches(rule.getPath_regex()) && List.of(rule.getMethods()).contains(method)
                )
                .findFirst()
                .orElse(null);
    }

    public static class Config {}
}
