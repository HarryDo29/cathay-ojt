package com.cathay.apigateway.filter;

import com.cathay.apigateway.core.routing.MatchResult;
import com.cathay.apigateway.data.config.LimitPropertiesConfig;
import com.cathay.apigateway.entity.EndpointHeaderRuleEntity;
import com.cathay.apigateway.entity.EndpointEntity;
import com.cathay.apigateway.entity.HeaderRuleEntity;
import com.cathay.apigateway.entity.MethodRuleEntity;
import com.cathay.apigateway.enums.Status;
import com.cathay.apigateway.service.EndpointHeaderRuleService;
import com.cathay.apigateway.service.EndpointService;
import com.cathay.apigateway.service.HeaderRuleService;
import com.cathay.apigateway.service.MethodRuleService;
import com.cathay.apigateway.util.ErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import jakarta.annotation.PostConstruct;
import javax.naming.AuthenticationException;
import java.util.*;

@Slf4j
@Component
public class ValidationGlobalGateway implements GlobalFilter, Ordered {
    private final LimitPropertiesConfig limitPropertiesConfig;
    private final EndpointService endpointRegisterService;
    private final MethodRuleService methodRuleService;
    private final HeaderRuleService headerRuleService;
    private final EndpointHeaderRuleService endpointHeaderRuleService;
    private final ErrorHandler errorHandler;

    // Caches - initialized in @PostConstruct to avoid circular dependency
    private Map<String, HeaderRuleEntity> headerRuleCache;
    private Map<String, List<EndpointHeaderRuleEntity>> endpointHeaderRuleCache;

    public ValidationGlobalGateway(
            LimitPropertiesConfig limitPropertiesConfig,
            EndpointService endpointRegisterService,
            MethodRuleService methodRuleService,
            HeaderRuleService headerRuleService,
            EndpointHeaderRuleService endpointHeaderRuleService,
            ErrorHandler errorHandler
    ) {
        this.limitPropertiesConfig = limitPropertiesConfig;
        this.endpointRegisterService = endpointRegisterService;
        this.methodRuleService = methodRuleService;
        this.headerRuleService = headerRuleService;
        this.endpointHeaderRuleService = endpointHeaderRuleService;
        this.errorHandler = errorHandler;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing validation caches...");
        // Initialize header rule cache
        this.headerRuleCache = headerRuleService.getHeaders();
        log.info("Loaded {} header rules", headerRuleCache.size());
        
        // Initialize endpoint header rule cache
        this.endpointHeaderRuleCache = endpointHeaderRuleService.getEndpointHeaderRule();
        log.info("Loaded endpoint header rules for {} endpoints", endpointHeaderRuleCache.size());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String method = req.getMethod().toString();
        String path = req.getURI().getPath();
        log.debug("Validating request: {} {}", method, path);

        // ============================================
        // PHASE 1: GLOBAL VALIDATION
        // ============================================
        // 1.1 Endpoint existed or not (or enabled or not)
        log.info("Path: {}, Method: {}", path, method);
        MatchResult result = endpointRegisterService.getEndpoint(path, method);
        log.info("Validating endpoint {} result: {}", path, result.toString());
        if (result.getStatus() != Status.FOUND) {
            log.warn("Endpoint not found: {} {}", method, path);
            return errorHandler.writeJsonError(exchange,
                    HttpStatus.NOT_FOUND,
                    path,
                    "Not Found",
                    "Endpoint not found");
        }
        
        EndpointEntity endpoint = result.getEntity();
        log.info("Endpoint: {}", endpoint);
        if (!endpoint.isEnabled()) {
            log.warn("Endpoint disabled: {} {}", method, path);
            return errorHandler.writeJsonError(exchange,
                    HttpStatus.NOT_FOUND,
                    path,
                    "Not Found",
                    "Endpoint not available");
        }

        // 1.2 Validate by HTTP method
        MethodRuleEntity methodRule = methodRuleService.getMethodRule(method);
        if (methodRule.isRequire_body()) {
            Mono<Void> bodyValidation = validateBody(exchange, req, methodRule);
            if (bodyValidation != null) return bodyValidation;
        }

        // 1.3 Validate query params count
        MultiValueMap<String, String> params = req.getQueryParams();
        if (params.size() > limitPropertiesConfig.getMax_query_params()) {
            log.warn("Too many query params: {} > {}", params.size(), limitPropertiesConfig.getMax_query_params());
            return errorHandler.writeJsonError(exchange,
                    HttpStatus.BAD_REQUEST,
                    path,
                    "Bad Request",
                    "Too many query parameters");
        }

        // 1.4 Validate request id header (add if missing)
        List<String> request_id = req.getHeaders()
                .get("X-Request-ID");
        if (request_id == null || request_id.isEmpty()) {
            log.info("Adding missing X-Request-ID header");
            ServerHttpRequest mutatedRequest = req.mutate()
                    .header("X-Request-ID", UUID.randomUUID().toString())
                    .build();
        }

        // ============================================
        // PHASE 2: ENDPOINT-SPECIFIC HEADER VALIDATION
        // ============================================
        HttpHeaders headers = req.getHeaders();
        String endpointId = endpoint.getId().toString();
        List<EndpointHeaderRuleEntity> endpointHeaderRules = 
            endpointHeaderRuleCache.getOrDefault(endpointId, Collections.emptyList());

        // Early return if no header rules for this endpoint
        if (endpointHeaderRules.isEmpty()) {
            log.debug("No header rules for endpoint: {} {}", method, path);
            return chain.filter(exchange);
        }

        // 2.1 Validate required headers
        for (EndpointHeaderRuleEntity rule : endpointHeaderRules) {
            if (rule.getRequired()) {
                HeaderRuleEntity headerRule = headerRuleCache.get(rule.getHeader_rule_id().toString());
                if (headerRule == null) {
                    log.error("Header rule not found: {}", rule.getHeader_rule_id());
                    continue;
                }
                
                String headerValue = headers.getFirst(headerRule.getName());
                if (headerValue == null || headerValue.isBlank()) {
                    log.warn("Missing required header '{}' for {} {}", headerRule.getName(), method, path);
                    return errorHandler.writeJsonError(exchange,
                            HttpStatus.UNAUTHORIZED,
                            path,
                            "Unauthorized",
                            "Missing required header: " + headerRule.getName());
                }
            }
        }

        // ============================================
        // PHASE 3: DETAILED HEADER VALIDATION
        // ============================================
        // Build set of allowed header IDs for this endpoint for O(1) lookup
        Map<String, HeaderRuleEntity> headerRules = new HashMap<>(Map.of());
        for (EndpointHeaderRuleEntity rule : endpointHeaderRules) {
            HeaderRuleEntity headerRule = headerRuleCache.get(rule.getHeader_rule_id().toString());
            if (headerRule != null) {
                headerRules.put(headerRule.getName().toLowerCase(), headerRule);
            }
        }

        // 3.1 Validate each present header
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String headerName = entry.getKey();
            List<String> headerValues = entry.getValue();
            
            if (headerValues == null || headerValues.isEmpty()) {
                continue;
            }

            // Get header rule by name (case-insensitive)
            HeaderRuleEntity headerRule = headerRules.get(headerName.toLowerCase());
            
            // Skip if header not in our rules (allow unknown headers)
            if (headerRule == null) {
                log.debug("Unknown header allowed: {}", headerName);
                continue;
            }

            // Validate each value of this header
            for (String headerValue : headerValues) {
                Mono<Void> headerValidation = validateHeaderValue(
                    exchange, headerName, headerValue, headerRule);
                if (headerValidation != null) return headerValidation;
            }
        }

        log.debug("Validation passed for: {} {}", method, path);
        return chain.filter(exchange);
    }

    private Mono<Void> validateBody(ServerWebExchange exchange, ServerHttpRequest req, MethodRuleEntity methodRule) {
        long length = req.getHeaders().getContentLength();
        long maxSize = methodRule.getMax_body_size();
        
        // Content-Length = -1 means not present or chunked encoding
        if (length <= 0) {
            log.warn("Missing or invalid Content-Length header");
            return errorHandler.writeJsonError(exchange,
                    HttpStatus.UNAUTHORIZED,
                    req.getPath().toString(),
                    "Bad request",
                    "Body required with valid Content-Length header");
        }
        
        if (length > maxSize) {
            log.warn("Payload too large: {} > {}", length, maxSize);
            return errorHandler.writeJsonError(exchange,
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    req.getPath().toString(),
                    "Bad request",
                    "Payload too large");
        }

        // Validate Content-Type if required
        if (methodRule.isRequire_content_type()) {
            MediaType contentType = req.getHeaders().getContentType();
            if (contentType == null ||
                (!contentType.isCompatibleWith(MediaType.APPLICATION_JSON) &&
                 !contentType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED) &&
                 !contentType.isCompatibleWith(MediaType.MULTIPART_FORM_DATA))) {
                log.warn("Unsupported Content-Type: {}", contentType);
                return errorHandler.writeJsonError(exchange,
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        req.getPath().toString(),
                        "Bad request",
                        "Unsupported Content-Type");
            }
        }
        
        return null; // No error
    }

    private Mono<Void> validateHeaderValue(
            ServerWebExchange exchange,
            String headerName,
            String headerValue,
            HeaderRuleEntity headerRule) {
        // 0. Check if header value exists and not empty
        if (headerValue == null || headerValue.isEmpty()) {
            log.debug("Header '{}' has empty value, skipping validation", headerName);
            return null; // Empty values already checked in required validation phase
        }

        // 1. Check for CRLF injection (most critical - check first)
        if (headerValue.contains("\r") || headerValue.contains("\n")) {
            log.warn("CRLF injection attempt in header '{}'", headerName);
            return errorHandler.writeJsonError(exchange,
                    HttpStatus.BAD_REQUEST,
                    exchange.getRequest().getPath().toString(),
                    "Bad request",
                    "Header '" + headerName + "' contains invalid characters");
        }

        // 2. Validate max length
        if (headerValue.length() > headerRule.getMax_length()) {
            log.warn("Header '{}' exceeds max length: {} > {}", 
                    headerName, headerValue.length(), headerRule.getMax_length());
            return errorHandler.writeJsonError(exchange,
                    HttpStatus.BAD_REQUEST,
                    exchange.getRequest().getPath().toString(),
                    "Bad request",
                    String.format("Header '%s' exceeds maximum length of %d characters (current: %d)",
                            headerName, headerRule.getMax_length(), headerValue.length())
            );
        }

        // 3. Validate pattern (regex) if configured
        String pattern = headerRule.getPattern();

        if (!headerValue.matches(pattern)) {
            log.warn("Header '{}' failed pattern validation. Value: {}",
                    headerName,
                    headerValue.length() > 50 ? headerValue.substring(0, 50) + "..." : headerValue);
            return errorHandler.writeJsonError(exchange,
                    HttpStatus.BAD_REQUEST,
                    exchange.getRequest().getPath().toString(),
                    "Bad request",
                    String.format("Header '%s' has invalid format. Expected: %s",
                            headerName, headerRule.getDescription())
            );
        }
        log.debug("Header '{}' passed pattern validation", headerName);
        return null; // No error
    }

    @Override
    public int getOrder() {
        return -20;
    }
}
