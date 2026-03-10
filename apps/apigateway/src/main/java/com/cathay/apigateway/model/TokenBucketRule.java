package com.cathay.apigateway.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

@Data
public class TokenBucketRule {
    private Integer replenish_rate;
    private Integer burst_capacity;
    private Integer ttl;

    public TokenBucketRule() {}

    // try to parse JSON to TokenBucketRule
    public static TokenBucketRule fromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, TokenBucketRule.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
