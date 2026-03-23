package com.cathay.apigateway.util;

import com.cathay.apigateway.dto.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class ErrorHandler {
    // Logger
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);
    // ObjectMapper for JSON serialization
    private final ObjectMapper objectMapper;

    public Mono<Void> writeError(ServerWebExchange exchange, Exception e, HttpStatus httpStatus) {
        String path = exchange.getRequest().getURI().getPath();
        
        if (e instanceof CallNotPermittedException) {
            logger.warn("[CircuitBreaker] Circuit OPEN for path: {} - {}", path, e.getMessage());
            return writeJsonError(exchange, HttpStatus.SERVICE_UNAVAILABLE, path,
                    "Circuit Breaker Open",
                    "Service is temporarily unavailable due to repeated failures. Please try again later.");
        }
        
        if (e instanceof TimeoutException) {
            logger.error("[Gateway] Timeout for path: {}", path);
            return writeJsonError(exchange, HttpStatus.GATEWAY_TIMEOUT, path,
                    "Gateway Timeout",
                    "The service took too long to respond");
        }
        
        if (e instanceof ConnectException) {
            logger.error("[Gateway] Connection failed for path: {}", path);
            return writeJsonError(exchange, HttpStatus.SERVICE_UNAVAILABLE, path,
                    "Service Unavailable",
                    "Unable to connect to the service");
        }
        
        logger.error("Gateway error: path={}, status={}, message={}", path, httpStatus, e.getMessage(), e);
        return writeJsonError(exchange, httpStatus, path,
                httpStatus.getReasonPhrase(),
                httpStatus.is5xxServerError() ? "Internal Server Error" : e.getMessage());
    }

    public Mono<Void> writeJsonError(ServerWebExchange exchange,
                                     HttpStatus httpStatus,
                                     String path,
                                     String error,
                                     String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(httpStatus.value());
        errorResponse.setError(error);
        errorResponse.setMessage(message);
        errorResponse.setPath(path);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException ex) {
            logger.error("Failed to serialize error response: {}", ex.getMessage());
            return response.setComplete();
        }
    }
}

