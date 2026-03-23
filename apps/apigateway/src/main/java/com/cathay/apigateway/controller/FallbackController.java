 package com.cathay.apigateway.controller;

 import lombok.extern.slf4j.Slf4j;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;
 import org.springframework.web.bind.annotation.GetMapping;
 import org.springframework.web.bind.annotation.PostMapping;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RestController;

 import java.time.Instant;
 import java.util.Map;

 @Slf4j
 @RestController
 @RequestMapping("/fallback")
 public class FallbackController {

     @GetMapping
     public ResponseEntity<Map<String, Object>> getFallback() {
         log.warn("[CircuitBreaker] Fallback triggered - GET request");
         return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                 .body(Map.of(
                         "error", "Service Temporarily Unavailable",
                         "message", "The service is currently experiencing issues. Please try again later.",
                         "timestamp", Instant.now().toString(),
                         "status", 503
                 ));
     }

     @PostMapping
     public ResponseEntity<Map<String, Object>> postFallback() {
         log.warn("[CircuitBreaker] Fallback triggered - POST request");
         return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                 .body(Map.of(
                         "error", "Service Temporarily Unavailable",
                         "message", "The service is currently experiencing issues. Please try again later.",
                         "timestamp", Instant.now().toString(),
                         "status", 503
                 ));
     }
 }
