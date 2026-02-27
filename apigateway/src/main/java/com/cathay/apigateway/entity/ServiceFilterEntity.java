package com.cathay.apigateway.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.UUID;

@Data
public class ServiceFilterEntity {
//    @Id
    private UUID id;

    private UUID serviceId;

    private UUID filterId;

    private String enabled;

    private Integer sort_order;

//    @CreatedDate
//    private LocalDateTime created_at;
//
//    @LastModifiedDate
//    private LocalDateTime updated_at;
}
