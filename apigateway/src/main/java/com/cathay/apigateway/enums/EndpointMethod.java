package com.cathay.apigateway.enums;

public enum EndpointMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH,
    HEAD,
    OPTIONS;

    public static EndpointMethod from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("HTTP method must not be null or empty");
        }

        try {
            return EndpointMethod.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported HTTP method: " + value);
        }
    }
}
