package com.cathay.apigateway.config;

import com.cathay.apigateway.entity.CircuitBreakerEntity;
import com.cathay.apigateway.service.CircuitBreakerService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
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
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCircuitBreakerCustomizer() {
        return factory -> {
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
//                        .slowCallDurationThreshold(Duration.parse("PT" + entity.getSlow_call_duration_threshold()))
                        .permittedNumberOfCallsInHalfOpenState(entity.getPermitted_number_of_calls_in_half_open_state())
                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.valueOf(entity.getSliding_window_type()))
                        .slidingWindowSize(entity.getSliding_window_size())
                        .minimumNumberOfCalls(entity.getMinimum_number_of_calls())
                        .waitDurationInOpenState(Duration.parse("PT" + entity.getWait_duration_in_open_state()))
                        .automaticTransitionFromOpenToHalfOpenEnabled(true)
                        .build();
                log.info("[CircuitBreaker] Using default config from YAML");
            } else {
                defaultConfig = CircuitBreakerConfig.custom()
                        .failureRateThreshold(35)
                        .slowCallRateThreshold(50)
//                        .slowCallDurationThreshold(Duration.parse("PT" + "1S"))
                        .permittedNumberOfCallsInHalfOpenState(5)
                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(50)
                        .minimumNumberOfCalls(25)
                        .waitDurationInOpenState(Duration.parse("PT" + "20S"))
                        .automaticTransitionFromOpenToHalfOpenEnabled(true)
                        .build();
                log.info("[CircuitBreaker] Using hardcoded default config");
            }

            factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                    .circuitBreakerConfig(defaultConfig)
                    .build());

//            for (CircuitBreakerEntity cb : circuitBreakerList) {
//                if (cb != null && cb.getService_id() != null && !"defaultCircuitBreaker".equalsIgnoreCase(cb.getName())) {
//                    CircuitBreakerConfig customConfig = CircuitBreakerConfig.custom()
//                            .failureRateThreshold(entity.getFailure_rate_threshold())
//                            .slowCallRateThreshold(entity.getSlow_call_rate_threshold())
//                            .slowCallDurationThreshold(Duration.parse("PT" + entity.getSlow_call_duration_threshold()))
//                            .permittedNumberOfCallsInHalfOpenState(entity.getPermitted_number_of_calls_in_half_open_state())
//                            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.valueOf(entity.getSliding_window_type()))
//                            .slidingWindowSize(entity.getSliding_window_size())
//                            .minimumNumberOfCalls(entity.getMinimum_number_of_calls())
//                            .waitDurationInOpenState(Duration.parse("PT" + entity.getWait_duration_in_open_state()))
//                            .build();
//
//                        factory.configure(builder -> new Resilience4JConfigBuilder(cb.getName())
//                            .circuitBreakerConfig(customConfig)
//                            .build(), cb.getName());
//
//                    log.info("[CircuitBreaker] Registered custom config for service: {}", cb.getName());
//                }
//            }

            factory.addCircuitBreakerCustomizer(cb -> {
                        cb.getEventPublisher()
                                .onStateTransition(event ->
                                log.warn("[CircuitBreaker] {} state changed: {} -> {}",
                                        event.getCircuitBreakerName(),
                                        event.getStateTransition().getFromState(),
                                        event.getStateTransition().getToState())
                                )
                                .onError(event ->
                                        log.error("[CircuitBreaker] {} error: {}",
                                                event.getCircuitBreakerName(),
                                                event.getThrowable().getMessage())
                                )
                                .onSuccess(event ->
                                        log.debug("[CircuitBreaker] {} success", event.getCircuitBreakerName())
                                );
                    }, "defaultCircuitBreaker", "orderServiceCircuitBreaker");
        };
    }
}
