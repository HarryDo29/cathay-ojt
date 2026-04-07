package com.cathay.apigateway.controller;

import com.cathay.apigateway.service.GatewayConfigReloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/internal/config")
@RequiredArgsConstructor
public class GatewayConfigController {
    private final GatewayConfigReloadService reloadService;

    @PostMapping("/reload")
    public Mono<Map<String, Object>> reload() {
        return reloadService.reloadAll()
                .thenReturn(Map.of(
                        "success", true,
                        "message", "Gateway config reloaded"
                ));
    }
}
