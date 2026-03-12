package com.cathay.apigateway.config;

import com.cathay.apigateway.model.SlideWindowRule;
import com.cathay.apigateway.model.SlidingWindowState;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CaffeineCacheConfig {

    @Bean
    public Cache<String, Bucket> ipRateLimitCache() {
        return Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterAccess(2, TimeUnit.MINUTES)
                .recordStats() // enable stats for monitoring
                .build();
    }

    @Bean
    public Cache<String, SlidingWindowState> emailRateLimitCache() {
        return Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterAccess(2, TimeUnit.MINUTES)
                .recordStats() // enable stats for monitoring
                .build();
    }

    @Bean
    public Cache<String, SlidingWindowState> accountRateLimitCache() {
        return Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterAccess(2, TimeUnit.MINUTES)
                .recordStats() // enable stats for monitoring
                .build();
    }
}
