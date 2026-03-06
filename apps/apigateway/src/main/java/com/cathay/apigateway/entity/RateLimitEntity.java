package com.cathay.apigateway.entity;

import com.cathay.apigateway.enums.KeyType;
import lombok.Data;

@Data
//@Entity
public class RateLimitEntity {
//    @Id
    private String id;

    private KeyType keyType; // e.g., IP, USER_ID, API_KEY

    private Integer replenishRate; // tokens added per second

    private Integer burstCapacity; // maximum tokens in the bucket

    private Integer ttl; // time to live for the rate limit configuration

    private Boolean enabled;
}
