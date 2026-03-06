package com.cathay.apigateway.entity;

import lombok.Data;

import java.util.UUID;

@Data
//@Table("allowed_origins")
public class AllowedOriginEntity {
//    @Id
    private UUID id;

    private String origin;

    private boolean enabled;
}
