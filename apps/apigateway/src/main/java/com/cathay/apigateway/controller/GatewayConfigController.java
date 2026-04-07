package com.cathay.apigateway.controller;

import com.cathay.apigateway.service.GatewayConfigReloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/internal/config")
@RequiredArgsConstructor
public class GatewayConfigController {
    private final GatewayConfigReloadService reloadService;

    @PostMapping("/reload")
    public Mono<ResponseEntity<Map<String, Object>>> reload() {
        return reloadService.reloadAll()
                .thenReturn(ResponseEntity.ok(Map.<String, Object>of(
                        "success", true,
                        "message", "Gateway config reloaded"
                )));
    }
}
