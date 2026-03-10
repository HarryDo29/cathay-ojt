package com.cathay.apigateway.data.config;

import com.cathay.apigateway.enums.KeyType;
import com.cathay.apigateway.enums.RateLimitType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.UUID;

@Data
@ConfigurationProperties(prefix = "app.ratelimit")
public class RateLimitConfig {
    public final List<RateLimit> rules;

    @Data
    public static class RateLimit{
        private UUID id;
        private String type; // e.g., SLIDE_WINDOW, TOKEN_BUCKET
        private String key_type; // e.g., IP, USER_ID, API_KEY
        private String rule; // JSON string for the rule
        private String enabled; // "true" or "false"
    }
}
