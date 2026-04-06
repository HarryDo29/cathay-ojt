package com.cathay.apigateway.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Table("services")
public class ServiceEntity {
    @Id
    private UUID id;

    private String name;

    private String path; // save as /api/v1/{service}/** service is name field

    private String url; // save as http://host:port

    private Integer strip_prefix;

    @Column("is_enabled")
    private boolean enabled;
}
