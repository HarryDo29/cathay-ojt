package com.cathay.apigateway.exception;

import com.cathay.apigateway.util.ErrorHandler;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
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
    private final ErrorHandler errorHandler;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getURI().getPath();

        switch (ex) {
            case CallNotPermittedException callNotPermittedException -> {
                log.warn("[CircuitBreaker] Circuit OPEN for path: {} - {}", path, ex.getMessage());
                return errorHandler.writeError(exchange,
                        callNotPermittedException,
                        HttpStatus.SERVICE_UNAVAILABLE);
            }
            case TimeoutException timeoutException -> {
                log.error("[Gateway] Timeout for path: {}", path);
                return errorHandler.writeError(exchange,
                        timeoutException,
                        HttpStatus.GATEWAY_TIMEOUT);
            }
            case ConnectException connectException -> {
                log.error("[Gateway] Connection failed for path: {}", path);
                return errorHandler.writeError(exchange,
                        connectException,
                        HttpStatus.SERVICE_UNAVAILABLE);
            }
            default -> {
            }
        }

        log.error("[Gateway] Unhandled error for path: {} - {}", path, ex.getMessage(), ex);
        return errorHandler.writeJsonError(exchange,
                HttpStatus.INTERNAL_SERVER_ERROR,
                path,
                "Internal Server Error",
                "An unexpected error occurred");
    }
}
