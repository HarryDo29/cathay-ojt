package com.cathay.apigateway.exception;

import com.cathay.apigateway.util.ErrorHandler;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {
    private final ErrorHandler errorHandler;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getURI().getPath();
        log.error("Error: {}", ex.getMessage());

        switch (ex) {
            case NotFoundException notFoundException -> {
                log.error("[Gateway] Not found for path: {}", path);
                return errorHandler.writeJsonError(exchange,
                        HttpStatus.NOT_FOUND,
                        path,
                        "Not Found Exception",
                        ex.getMessage());
            }
            case CallNotPermittedException callNotPermittedException -> {
                log.warn("[CircuitBreaker] Circuit OPEN for path: {} - {}", path, ex.getMessage());
                return errorHandler.writeJsonError(exchange,
                        HttpStatus.SERVICE_UNAVAILABLE,
                        path,
                        "Call Not Permitted Exception",
                        ex.getMessage());
            }
            case TimeoutException timeoutException -> {
                log.error("[Gateway] Timeout for path: {}", path);
                return errorHandler.writeJsonError(exchange,
                        HttpStatus.GATEWAY_TIMEOUT,
                        path,
                        "Time Out Exception",
                        ex.getMessage());
            }
            case ConnectException connectException -> {
                log.error("[Gateway] Connection failed for path: {}", path);
                return errorHandler.writeJsonError(exchange,
                        HttpStatus.SERVICE_UNAVAILABLE,
                        path,
                        "Connection Exception",
                        ex.getMessage());
            }
            default -> {
            }
        }

        log.error("[Gateway] Unhandled error for path: {} - {}", path, ex.getMessage());
        return errorHandler.writeJsonError(exchange,
                HttpStatus.INTERNAL_SERVER_ERROR,
                path,
                "Internal Server Error",
                "An unexpected error occurred");
    }
}
