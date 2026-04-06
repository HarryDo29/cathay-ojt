package com.cathay.apigateway.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Data
@Table("service_filters")
public class ServiceFilterEntity {
    @Id
    private UUID id;

    private UUID serviceId;

    private UUID filterId;

    @Column("is_enabled")
    private String enabled;

    private Integer sort_order;
}
