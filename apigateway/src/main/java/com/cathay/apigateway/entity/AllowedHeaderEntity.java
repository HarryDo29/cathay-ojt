package com.cathay.apigateway.entity;

import lombok.Data;

import java.util.UUID;

@Data
//@Table("allowed_headers")
public class AllowedHeaderEntity {
//    @Id
    private UUID id;

    private String header;

    private Boolean enabled;
}
