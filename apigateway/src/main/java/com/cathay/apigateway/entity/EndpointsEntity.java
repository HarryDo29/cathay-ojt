package com.cathay.apigateway.entity;

import lombok.Data;

import java.util.UUID;

//@Table("endpoints")
@Data
public class EndpointsEntity {
//    @Id
    private UUID id;

    private String path;

    private String method;

//    @Column("service_id")
    private String serviceId;

//    @Column("is_enabled")
    private boolean enabled;

//    @Column("is_public")
    private boolean isPublic;

//    @Column("min_role_level")
    private int minRoleLevel;

//    @CreatedDate
//    private LocalDateTime created_at;
//
//    @LastModifiedDate
//    private LocalDateTime updated_at;
}
