package com.cathay.apigateway.data.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.UUID;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.cors")
public class CorsConfig {
    List<AllowedOrigin> allowedOrigins;
    List<AllowedHeader> allowedHeaders;

    @Data
    public static class AllowedOrigin {
        private UUID id;
        private String origin;
        private String enabled;
    }

    @Data
    public static class AllowedHeader {
        private UUID id;
        private String header;
        private String enabled;
    }
}
