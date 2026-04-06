package com.cathay.apigateway.filter;

import com.cathay.apigateway.enums.KeyType;
import com.cathay.apigateway.model.SlideWindowRule;
import com.cathay.apigateway.model.ManualSlidingWindow;
import com.cathay.apigateway.service.RateLimitRuleService;
import com.cathay.apigateway.util.CacheUtil;
import com.cathay.apigateway.util.ErrorHandler;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Slf4j
@Component
public class AccountBasedRateLimitGatewayFilterFactory
    extends AbstractGatewayFilterFactory<AccountBasedRateLimitGatewayFilterFactory.Config> {
    private static final String ACCOUNT_RATE_LIMIT_PATTERN = "Rate_Limit:ACCOUNT:%s:%s:%s";

    private final RateLimitRuleService rateLimitService;
    private final CacheUtil cacheUtil;
    private final Cache<String, ManualSlidingWindow> accountRateLimitCache;
    private final ErrorHandler errorHandler;

    public AccountBasedRateLimitGatewayFilterFactory(RateLimitRuleService rateLimitService,
                                                     CacheUtil cacheUtil,
                                                     Cache<String, ManualSlidingWindow> accountRateLimitCache,
                                                     ErrorHandler errorHandler) {
        super(Config.class);
        this.rateLimitService = rateLimitService;
        this.cacheUtil = cacheUtil;
        this.accountRateLimitCache = accountRateLimitCache;
        this.errorHandler = errorHandler;
    }

    public boolean tryAccess(String accountId, String uri, SlideWindowRule rule) {
        String cacheKey = buildCacheKey(accountId, uri, rule);
        if (this.cacheUtil.checkAccountRateLimitCache(cacheKey, rule)){
            cacheUtil.logRateLimitDetail(KeyType.ACCOUNT_ID, cacheKey, accountRateLimitCache);
            return true;
        }
        return false;
    }

    private static String buildCacheKey(String accountId, String uri, SlideWindowRule rule) {
        String servicePart = Arrays.stream(uri.split("/"))
                .filter(r -> r.contains("service"))
                .findFirst()
                .orElse("unknown_service");
        return String.format(ACCOUNT_RATE_LIMIT_PATTERN,
                accountId, servicePart, String.join(",", rule.getMethods()));
    }

    @Override
    public GatewayFilter apply(AccountBasedRateLimitGatewayFilterFactory.Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            log.info("AccountBasedRateLimitGatewayFilterFactory - request URI: {}", request.getURI());

            String isPublicEndpoint = request.getHeaders().getFirst("Public-Endpoint");
            if (Boolean.parseBoolean(isPublicEndpoint)) {
                log.info("Public endpoint detected, skipping account-based rate limit.");
                return chain.filter(exchange);
            }

            String accountId = request.getHeaders().getFirst("X-User-Id");
            if (accountId == null || accountId.isEmpty()) {
                log.error("Missing X-User-Id header for request: {}", request.getURI());
                return errorHandler.writeError(exchange,
                        new IllegalArgumentException("Missing X-User-Id header"),
                        HttpStatus.FORBIDDEN);
            }

            String uri = request.getURI().getPath();
            String method = request.getMethod().name();
            SlideWindowRule rule = findAccountRateLimitConfig(uri, method);
            if (rule == null) {
                log.error("No account-based rate limit configuration for {} {}, allowing request", method, uri);
                return chain.filter(exchange);
            }

            if (this.tryAccess(accountId, uri, rule)) {
                log.trace("Account {} allowed by sliding window for {} {}", accountId, method, uri);
                return chain.filter(exchange);
            }

            log.warn("Account {} blocked by sliding window (limit {} per {}s) for {} {}",
                                        accountId, rule.getLimit(), rule.getWindow(), method, uri);
            return errorHandler.writeError(exchange,
                    new RuntimeException("Account Rate Limit exceeded"),
                    HttpStatus.TOO_MANY_REQUESTS);
        };
    }

    private SlideWindowRule findAccountRateLimitConfig(String uri, String method) {
        return rateLimitService.getSlideWindowRuleList()
                .stream()
                .filter(rule ->
                    uri.matches(rule.getPath_regex()) && rule.getMethods().contains(method)
                )
                .findFirst()
                .orElse(null);
    }

    public static class Config {}
}
