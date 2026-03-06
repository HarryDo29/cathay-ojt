package com.cathay.apigateway.data.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;
import java.util.UUID;

@Data
@ConfigurationProperties(prefix = "app.headers")
public class HeaderRuleConfig {
    private List<HeaderRule> header_rules;
    private List<EndpointHeaderRule> endpoint_header_rules;

    @Data
    public static class HeaderRule {
        private UUID id;
        private String name;
        private Integer max_length;
        private String pattern;
        private String description;
    }

    @Data
    public static class EndpointHeaderRule {
        private UUID id;
        private UUID endpoint_id;
        private UUID header_rule_id;
        private String required;
    }
}
