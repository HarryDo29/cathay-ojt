package com.cathay.apigateway.config;

import com.cathay.apigateway.entity.MethodRuleEntity;
import com.cathay.apigateway.service.AllowedHeaderService;
import com.cathay.apigateway.service.AllowedOriginService;
import com.cathay.apigateway.service.MethodRuleService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Objects;


@Configuration
public class CorsConfig {
    private final AllowedOriginService allowedOriginService;
    private final MethodRuleService methodRuleService;
    private final AllowedHeaderService allowedHeaderService;

    public CorsConfig(AllowedOriginService allowedOriginService, MethodRuleService methodRuleService,
                      AllowedHeaderService allowedHeaderService) {
        this.allowedOriginService = allowedOriginService;
        this.methodRuleService = methodRuleService;
        this.allowedHeaderService = allowedHeaderService;
    }

    @Bean
    public CorsWebFilter corsGatewayFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // Get Allowed Origin
        List<String> allowedOrigins = allowedOriginService.getAllowedOriginList()
                .stream()
                .map(origin -> origin.isEnabled() ? origin.getOrigin() : null)
                .filter(Objects::nonNull)
                .toList();
        corsConfiguration.addAllowedOrigin(String.valueOf(allowedOrigins));
        // Get Allowed Method
        List<String> allowedMethods = methodRuleService.getMethodRuleSet()
                .stream()
                .map(MethodRuleEntity::getMethod)
                .filter(Objects::nonNull)
                .toList();
        corsConfiguration.addAllowedMethod(String.valueOf(allowedMethods));
        // Get Allowed Header
        List<String> allowedHeaders = allowedHeaderService.getAllowedHeaderList()
                .stream()
                .map(header -> header.getEnabled() ? header.getHeader() : null)
                .filter(Objects::nonNull)
                .toList();
        corsConfiguration.addAllowedHeader(String.valueOf(allowedHeaders));
        // Get Headers Exposed
        corsConfiguration.addAllowedHeader("*"); 
        // Expose all headers in header response (will change after testing)
        corsConfiguration.setAllowCredentials(true); // Allow credentials such as cookies
        corsConfiguration.setMaxAge(3600L); // Cache pre-flight response for 1 hour
        // Register CORS configuration
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        
        return new CorsWebFilter(source);
    }
}
