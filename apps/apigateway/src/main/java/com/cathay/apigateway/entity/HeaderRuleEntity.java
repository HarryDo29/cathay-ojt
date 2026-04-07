package com.cathay.apigateway.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;


@Data
@NoArgsConstructor
@Table("header_rules")
public class HeaderRuleEntity {
    @Id
    private UUID id;

    private String name;

    @NonNull
    private Integer max_length;

    @NonNull
    private String pattern;

    private String description;
}
