package com.cathay.apigateway.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@NoArgsConstructor
@Table("filters")
public class FilterEntity {
    @Id
    private UUID id;

    private String name;

    private String description;

    private String status;
}
