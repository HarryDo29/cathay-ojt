package com.cathay.apigateway.entity;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Table("allowed_headers")
public class AllowedHeaderEntity {
    @Id
    private UUID id;

    @NonNull
    private String header;

    @NonNull
    @Column("is_enabled")
    private Boolean enabled;
}
