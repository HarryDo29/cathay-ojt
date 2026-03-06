package com.cathay.apigateway.entity;

import lombok.Data;

import java.util.UUID;

@Data
public class RoleEntity {
//    @Id
    private UUID id;

    private String name;

    private String description;

    private Integer level;

    private boolean enabled;

//    @CreatedDate
//    private LocalDateTime created_at;
//
//    @LastModifiedDate
//    private LocalDateTime updated_at;
}
