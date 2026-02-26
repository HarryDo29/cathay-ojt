package com.cathay.apigateway.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum KeyType {
    IP("IP"),
    ACCOUNT_ID("ACCOUNT");

    private final String value;

    KeyType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static KeyType fromValue(String value) {
        return Arrays.stream(values())
                .filter(k -> k.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown KeyType: " + value));
    }
}
