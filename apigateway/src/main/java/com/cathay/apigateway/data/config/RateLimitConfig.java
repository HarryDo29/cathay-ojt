package com.cathay.apigateway.data.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.ratelimit")
public class RateLimitConfig {
    public final List<RateLimit> rules;

    @Data
    public static class RateLimit{
        private String id;
        private String key_type; // e.g., IP, USER_ID, API_KEY
        private Integer replenish_rate; // tokens added per second
        private Integer burst_capacity; // maximum tokens in the bucket
        private Integer ttl;
        private String enabled; // "Y" or "N"
    }
}
