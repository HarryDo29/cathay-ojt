package com.cathay.apigateway.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;


@Data
@NoArgsConstructor
@Table("endpoints")
public class EndpointEntity {
    @Id
    private UUID id;

    @NonNull
    private String path;

    private String method;

    @Column("service_id")
    private String serviceId;

    @Column("is_enabled")
    private boolean enabled;

    @Column("is_public")
    private boolean isPublic;

    @Column("min_role_level")
    private int minRoleLevel;
}
