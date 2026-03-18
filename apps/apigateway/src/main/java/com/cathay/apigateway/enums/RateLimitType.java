package com.cathay.apigateway.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum RateLimitType {
    TOKEN_BUCKET("TOKEN_BUCKET"),
    SLIDING_WINDOW("SLIDING_WINDOW");

    private final String value;

    RateLimitType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RateLimitType fromValue(String value) {
        return Arrays.stream(values())
                .filter(k -> k.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown RateLimitType: " + value));
    }
}
