package com.cathay.apigateway.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@NoArgsConstructor
@Table("roles")
public class RoleEntity {
    @Id
    private UUID id;

    private String name;

    private String description;

    private Integer level;

    @Column("is_enabled")
    private boolean enabled;
}
