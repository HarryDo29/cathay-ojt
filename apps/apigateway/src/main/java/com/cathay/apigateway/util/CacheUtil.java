package com.cathay.apigateway.util;

import com.cathay.apigateway.enums.KeyType;
import com.cathay.apigateway.model.ManualSlidingWindow;
import com.cathay.apigateway.model.ManualTokenBucket;
import com.cathay.apigateway.model.SlideWindowRule;
import com.cathay.apigateway.model.TokenBucketRule;
import com.github.benmanes.caffeine.cache.Cache;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
public class CacheUtil {
    private final Cache<String, ManualTokenBucket> ipRateLimitCache;
    private final Cache<String, ManualSlidingWindow> emailRateLimitCache;
    private final Cache<String, ManualSlidingWindow> accountRateLimitCache;

    public CacheUtil(Cache<String, ManualTokenBucket> ipRateLimitCache,
                     Cache<String, ManualSlidingWindow> emailRateLimitCache,
                     Cache<String, ManualSlidingWindow> accountRateLimitCache) {
        this.ipRateLimitCache = ipRateLimitCache;
        this.emailRateLimitCache = emailRateLimitCache;
        this.accountRateLimitCache = accountRateLimitCache;
    }

    public boolean checkIpRateLimit(String key, TokenBucketRule rule) {
        ManualTokenBucket bucket = this.ipRateLimitCache.get(key,
                k -> new ManualTokenBucket(rule.getBurst_capacity(), rule.getReplenish_rate())
        );
        return bucket.tryConsume(1L);
    }

    public boolean checkEmailRateLimit(String key, SlideWindowRule rule) {
        ManualSlidingWindow bucket = this.emailRateLimitCache.get(key,
                k -> new ManualSlidingWindow(rule.getWindow(), Duration.ofMillis(rule.getLimit()))
        );
        return bucket.tryConsume();
    }

    public boolean checkAccountRateLimitCache(String key, SlideWindowRule rule) {
        ManualSlidingWindow bucket = this.accountRateLimitCache.get(key,
                k -> new ManualSlidingWindow(rule.getWindow(), Duration.ofMillis(rule.getLimit()))
        );
        return bucket.tryConsume();
    }

    public <T> void logRateLimitDetail(KeyType type, String key, Cache<String, T> cache) {
        T value = cache.getIfPresent(key);

        if (value == null) {
            log.warn("[{}] Rate Limit Hit for Key: {} | Nhưng không tìm thấy dữ liệu trong cache (có thể vừa bị expire)", type, key);
            return;
        }

        StringBuilder detail = new StringBuilder();
        detail.append(String.format("\n--- RATE LIMIT HIT: %s ---", type));
        detail.append(String.format("\nKey: %s", key));
        detail.append(String.format("\nTime: %s", Instant.now()));

        if (value instanceof ManualTokenBucket manual) {
            manual.logging(detail);
        } else if (value instanceof ManualSlidingWindow manual) {
            manual.logging(detail);
        }

        // In thêm toàn bộ object trong cache (dựa trên toString())
        detail.append(String.format("\nRaw cache value: %s", value));
        detail.append("\n------------------------------");
        System.out.println(detail);
    }

}
