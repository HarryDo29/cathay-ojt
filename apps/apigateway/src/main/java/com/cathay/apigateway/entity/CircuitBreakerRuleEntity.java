package com.cathay.apigateway.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@NoArgsConstructor
@Table("circuit_breaker_rules")
public class CircuitBreakerRuleEntity {
    @Id
    private UUID id;

    @Column("service_id")
    private UUID service_id;

    private Boolean enabled;

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
