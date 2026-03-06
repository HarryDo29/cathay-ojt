package com.cathay.apigateway.data.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;
import java.util.UUID;

@Data
@ConfigurationProperties(prefix = "app.methods")
public class MethodRulesConfig {
    private List<MethodRule> method_rules;

    @Data
    public static class MethodRule {
        private UUID id;
        private String method;
        private boolean require_body;
        private boolean require_content_type;
        private long max_body_size;
    }
}
