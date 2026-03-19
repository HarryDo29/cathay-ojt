package com.cathay.apigateway.config;

import com.cathay.apigateway.entity.CircuitBreakerEntity;
import com.cathay.apigateway.service.CircuitBreakerService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Slf4j
@Configuration
public class GatewayCircuitBreakerConfig {
    private final CircuitBreakerService circuitBreakerService;

    public GatewayCircuitBreakerConfig(CircuitBreakerService circuitBreakerService) {
        this.circuitBreakerService = circuitBreakerService;
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        List<CircuitBreakerEntity> circuitBreakerList = circuitBreakerService.getCircuitBreakerList();
        CircuitBreakerEntity entity = circuitBreakerList.stream()
                .filter(cb -> cb.getName().equals("defaultCircuitBreaker"))
                .findFirst()
                .orElse(null);
        CircuitBreakerConfig defaultConfig;
        if (entity != null) {
             defaultConfig = CircuitBreakerConfig.custom()
                    .failureRateThreshold(entity.getFailure_rate_threshold())
                    .slowCallRateThreshold(entity.getSlow_call_rate_threshold())
                    .slowCallDurationThreshold(Duration.parse("PT" + entity.getSlow_call_duration_threshold()))
                    .permittedNumberOfCallsInHalfOpenState(entity.getPermitted_number_of_calls_in_half_open_state())
                    .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.valueOf(entity.getSliding_window_type()))
                    .slidingWindowSize(entity.getSliding_window_size())
                    .minimumNumberOfCalls(entity.getMinimum_number_of_calls())
                    .waitDurationInOpenState(Duration.parse("PT" + entity.getWait_duration_in_open_state()))
                    .build();
             log.info("Default Circuit breaker config loaded successfully");
        }else {
            defaultConfig = CircuitBreakerConfig.custom()
                    .failureRateThreshold(35)
                    .slowCallRateThreshold(50)
                    .slowCallDurationThreshold(Duration.parse("PT" + "1S"))
                    .permittedNumberOfCallsInHalfOpenState(5)
                    .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                    .slidingWindowSize(50)
                    .minimumNumberOfCalls(25)
                    .waitDurationInOpenState(Duration.parse("PT" + "20S"))
                    .build();
        }

        CircuitBreakerRegistry.Builder registryBuilder = CircuitBreakerRegistry.custom()
                .withCircuitBreakerConfig(defaultConfig);

        for (CircuitBreakerEntity cb : circuitBreakerList) {
            if (cb.getName().equals("defaultCircuitBreaker")) {
                CircuitBreakerConfig customConfig = CircuitBreakerConfig.custom()
                        .failureRateThreshold(cb.getFailure_rate_threshold())
                        .slowCallRateThreshold(cb.getSlow_call_rate_threshold())
                        .slowCallDurationThreshold(Duration.parse("PT" + cb.getSlow_call_duration_threshold()))
                        .permittedNumberOfCallsInHalfOpenState(cb.getPermitted_number_of_calls_in_half_open_state())
                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.valueOf(cb.getSliding_window_type()))
                        .slidingWindowSize(cb.getSliding_window_size())
                        .minimumNumberOfCalls(cb.getMinimum_number_of_calls())
                        .waitDurationInOpenState(Duration.parse("PT" + cb.getWait_duration_in_open_state()))
                        .build();

                registryBuilder.addCircuitBreakerConfig(cb.getName(), customConfig);
                log.info("[CircuitBreaker] Registered custom config for service: {}", cb.getName());
            }
        }

        return registryBuilder.build();
    }
}
