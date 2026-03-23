package com.cathay.apigateway.exception;

import com.cathay.apigateway.dto.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getURI().getPath();

        switch (ex) {
            case CallNotPermittedException callNotPermittedException -> {
                log.warn("[CircuitBreaker] Circuit OPEN for path: {} - {}", path, ex.getMessage());
                return writeErrorResponse(exchange,
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Circuit Breaker Open",
                        "Service is temporarily unavailable due to repeated failures. Please try again later.",
                        path);
            }
            case TimeoutException timeoutException -> {
                log.error("[Gateway] Timeout for path: {}", path);
                return writeErrorResponse(exchange,
                        HttpStatus.GATEWAY_TIMEOUT,
                        "Gateway Timeout",
                        "The service took too long to respond",
                        path);
            }
            case ConnectException connectException -> {
                log.error("[Gateway] Connection failed for path: {}", path);
                return writeErrorResponse(exchange,
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Service Unavailable",
                        "Unable to connect to the service",
                        path);
            }
            default -> {
            }
        }

        log.error("[Gateway] Unhandled error for path: {} - {}", path, ex.getMessage(), ex);
        return writeErrorResponse(exchange,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An unexpected error occurred",
            path);
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, 
                                          HttpStatus status,
                                          String error,
                                          String message,
                                          String path) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(status.value());
        errorResponse.setError(error);
        errorResponse.setMessage(message);
        errorResponse.setPath(path);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize error response", e);
            return exchange.getResponse().setComplete();
        }
    }
}
