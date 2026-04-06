package com.cathay.apigateway.entity;

import lombok.Data;
import org.checkerframework.checker.units.qual.C;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;


@Data
@Table("endpoint_header_rules")
public class EndpointHeaderRuleEntity {
    @Id
    private UUID id;

    @Column("endpoint_id")
    private UUID endpoint_id;

    @Column("header_rule_id")
    private UUID header_rule_id;

    @Column("is_required")
    private Boolean required;
}
