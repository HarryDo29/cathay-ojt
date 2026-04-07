package com.cathay.apigateway.entity;

import com.cathay.apigateway.enums.KeyType;
import com.cathay.apigateway.enums.RateLimitType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@NoArgsConstructor
@Table("rate_limit_rules")
public class RateLimitRuleEntity {
    @Id
    private UUID id;

    private RateLimitType type; // e.g., TOKEN_BUCKET, SLIDING_WINDOW

    private KeyType keyType; // e.g., IP, ACCOUNT_ID

    private String rule; // rule of rate limiting depends on the rate type

    @Column("is_enabled")
    private Boolean enabled;
}
