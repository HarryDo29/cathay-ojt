package com.cathay.apigateway.data.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import java.util.UUID;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.routes")
public class ServiceConfig {
    private List<RouteService> services;
    private List<RouteFilters> filters;
    private List<ServiceFilters> serviceFilters;

    @Data
    public static class RouteService {
        private UUID id;
        private String name;
        private String path;
        private String url;
        private String strip_prefix;
        private String enabled;
    }

    @Data
    public static class RouteFilters{
        private UUID id;
        private String name;
        private String description;
        private String status;
    }

    @Data
    public static class ServiceFilters{
        private UUID id;
        private UUID serviceId;
        private UUID filterId;
        private String enabled;
        private String sort_order;
    }
}
