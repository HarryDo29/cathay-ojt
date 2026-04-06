package com.cathay.apigateway.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;


@Data
@NoArgsConstructor
@Table("method_rules")
public class MethodRuleEntity {
    @Id
    private UUID id;

    private String method;

    private boolean require_body;

    private boolean require_content_type;

    private long max_body_size;
}
