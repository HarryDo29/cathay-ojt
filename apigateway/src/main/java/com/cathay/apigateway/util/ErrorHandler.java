package com.cathay.apigateway.util;

import com.cathay.apigateway.dto.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Component
@RequiredArgsConstructor
public class ErrorHandler {
    // Logger
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);
    // ObjectMapper for JSON serialization
    private final ObjectMapper objectMapper;

    public Mono<Void> writeError(ServerWebExchange exchange, Exception e, HttpStatus httpStatus) {
        String path = exchange.getRequest().getURI().getPath();
        logger.error("Gateway error: path={}, status={}, message={}", path, httpStatus, e.getMessage(), e);
        // Prepare the response
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // Create ErrorResponse object (error response body)
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(httpStatus.value());
        errorResponse.setError(httpStatus.getReasonPhrase());
        errorResponse.setMessage(httpStatus.is5xxServerError() ? "Internal Server Error" : e.getMessage());
        errorResponse.setPath(path);
        // Serialize and write the response
        try {
            // Serialize ErrorResponse to JSON
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException ex) {
            // Fallback in case of serialization error
            logger.error("Failed to serialize error response: {}", ex.getMessage());
            return response.setComplete();
        }
    }
}
