package com.cathay.apigateway.entity;

import lombok.Data;

import java.util.UUID;

//@Table("endpoint_header_rules")
@Data
public class EndpointHeaderRuleEntity {
//    @Id
    private UUID id;

    private UUID endpoint_id;

    private UUID header_rule_id;

    private boolean required;

    // Reference to the actual header rule (for convenience)
//    private String headerName;
//    private Integer maxLength;
}
