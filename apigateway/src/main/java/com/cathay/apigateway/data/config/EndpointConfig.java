package com.cathay.apigateway.data.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.UUID;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.gateway")
public class EndpointConfig {
    List<Endpoint> endpoints;

    @Data
    public static class Endpoint {
        private UUID id;
        private String path;
        private String method;
        private String serviceId;
        private String isPublic;
        private int min_role_level;
        private String enabled;
    }

}
