package com.cathay.apigateway.data.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.UUID;

@Data
@ConfigurationProperties(prefix = "app.security")
public class RoleConfig {
    private final List<Role> roles;

    @Data
    public static class Role {
        private UUID id;
        private String name;
        private String description;
        private Integer level;
        private String enabled;
    }
}
