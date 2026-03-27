package com.cathay.apigateway.entity;

import lombok.Data;
import java.util.UUID;

@Data
//@Entity
public class CircuitBreakerEntity {
//    @Id
    private UUID id;

    private UUID service_id;

    private boolean enabled;

    private int failure_rate_threshold;

    private int slow_call_rate_threshold;

    private String slow_call_duration_threshold; // 1s

    private int permitted_number_of_calls_in_half_open_state;

    private String sliding_window_type; //COUNT_BASED

    private int sliding_window_size;

    private int minimum_number_of_calls;

    private String wait_duration_in_open_state; // 20s

    private String name;
}
