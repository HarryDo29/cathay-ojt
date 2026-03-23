package com.cathay.apigateway.filter;

import com.cathay.apigateway.entity.RateLimitEntity;
import com.cathay.apigateway.enums.KeyType;
import com.cathay.apigateway.enums.RateLimitType;
import com.cathay.apigateway.model.TokenBucketRule;
import com.cathay.apigateway.service.RateLimitService;
import com.cathay.apigateway.util.CacheUtil;
import com.cathay.apigateway.util.ErrorHandler;
import com.cathay.apigateway.model.ManualTokenBucket;
import com.cathay.apigateway.util.RequestUtil;
import com.github.benmanes.caffeine.cache.Cache;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class IPBasedRateLimitGlobalFilter implements GlobalFilter, Ordered {
    private static final int ORDER = -100;
    private static final String BLACK_LIST_PATTERN = "Black_List:IP:%s";
    private static final String IP_RATE_LIMIT_PATTERN = "Rate_Limit:IP:%s";
    private static final String ABUSE_COUNTER_PATTERN = "Abuse_Counter:IP:%s";

    private final RateLimitService rateLimitService;
    private final ErrorHandler errorHandler;
    private final Cache<String, ManualTokenBucket> ipRateLimitCache;
    private final CacheUtil cacheUtil;

//    private Bucket createNewBucket(TokenBucketRule rule) {
//        Bandwidth limit = Bandwidth.builder()
//                .capacity(rule.getBurst_capacity()) // set capacity
//                .refillIntervally(
//                        rule.getReplenish_rate(), // set replenish rate
//                        Duration.ofMinutes(rule.getTtl()) // set ttl
//                )// sau ttl thì nạp lại replenish_rate token vào bucket
//                .build();
//        return Bucket.builder()
//                .addLimit(limit)
//                .build();
//    }
//
//    public boolean tryAccess(String key, TokenBucketRule rule) {
//        Bucket bucket = ipRateLimitCache.get(key, k -> createNewBucket(rule));
//        return bucket.tryConsume(1);
//    }

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
            return chain.filter(exchange);
        }
        log.warn("IP {} blocked by rate limit rule", ip);
        return errorHandler.writeError(exchange,
                new NotFoundException("Rate limit exceeded"), HttpStatus.TOO_MANY_REQUESTS);
    }

    private boolean tryAccess(String ip, TokenBucketRule rule){
        String blackListKey = String.format(BLACK_LIST_PATTERN, ip);
        if (cacheUtil.checkBlackListCache(blackListKey)){
            log.warn("IP {} blocked by black list", ip);
            return false;
        }
        String rateLimitKey = this.buildCacheKey(ip);
        if (cacheUtil.checkIpRateLimitCache(rateLimitKey, rule)){
            cacheUtil.logRateLimitDetail(KeyType.IP, rateLimitKey, ipRateLimitCache);
            log.info("IP {} allowed by rate limit rule", ip);
            return true;
        }

        // Rate limit hit
        log.warn("IP {} blocked by rate limit rule", ip);
        String abuseKey = this.buildAbuseKey(ip);
        boolean abuseCounterOk = cacheUtil.checkAbuseCounterCache(abuseKey);
        if (!abuseCounterOk) {
            cacheUtil.addToBlackListCache(blackListKey);
        }
        return false;
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

    private String buildAbuseKey(String ip){
        return String.format(ABUSE_COUNTER_PATTERN, ip);
    }

    private String buildCacheKey(String ip) {
        return String.format(IP_RATE_LIMIT_PATTERN, ip);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
