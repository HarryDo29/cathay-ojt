package com.cathay.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    @Primary
    public ReactiveRedisTemplate<String, String> redisTemplate(ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer serializer = new StringRedisSerializer();
        RedisSerializationContext<String, String> context = RedisSerializationContext
                .<String, String>newSerializationContext(serializer)
                .value(serializer)
                .hashKey(serializer)
                .hashValue(serializer)
                .build();
        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public RedisScript<Long> ipRateLimitLuaScript() {
        ClassPathResource resource = new ClassPathResource("script/ip_rate_limit.lua");
        return RedisScript.of(resource, Long.class);
    }

    @Bean
    public RedisScript<Long> accountRateLimitLuaScript() {
        ClassPathResource resource = new ClassPathResource("script/account_rate_limit.lua");
        return RedisScript.of(resource, Long.class);
    }
}
