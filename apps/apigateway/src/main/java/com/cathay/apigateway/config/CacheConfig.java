package com.cathay.apigateway.config;

import com.cathay.apigateway.model.ManualSlidingWindow;
import com.cathay.apigateway.model.ManualTokenBucket;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, ManualTokenBucket> ipRateLimitCache() {
        return Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterAccess(2, TimeUnit.MINUTES)
                .recordStats() // enable stats for monitoring
                .build();
    }

    @Bean
    public Cache<String, ManualSlidingWindow> emailRateLimitCache() {
        return Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterAccess(2, TimeUnit.MINUTES)
                .recordStats() // enable stats for monitoring
                .build();
    }

    @Bean
    public Cache<String, ManualSlidingWindow> accountRateLimitCache() {
        return Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterAccess(2, TimeUnit.MINUTES)
                .recordStats() // enable stats for monitoring
                .build();
    }
}
