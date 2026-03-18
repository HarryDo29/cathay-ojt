package com.cathay.apigateway.entity;

import com.cathay.apigateway.enums.KeyType;
import com.cathay.apigateway.enums.RateLimitType;
import lombok.Data;

import java.util.UUID;

@Data
//@Entity
public class RateLimitEntity {
//    @Id
    private UUID id;

    private RateLimitType type; // e.g., TOKEN_BUCKET, SLIDING_WINDOW

    private KeyType keyType; // e.g., IP, ACCOUNT_ID

    private String rule; // rule of rate limiting depends on the rate type

    private Boolean enabled;
}
