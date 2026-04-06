package com.cathay.apigateway.entity;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;


@Data
@Table("header_rules")
public class HeaderRulesEntity {
    @Id
    private UUID id;

    private String name;

    @NonNull
    private Integer max_length;

    @NonNull
    private String pattern;

    private String description;
}
