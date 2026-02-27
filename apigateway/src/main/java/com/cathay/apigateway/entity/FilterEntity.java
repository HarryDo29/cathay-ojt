package com.cathay.apigateway.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.UUID;

@Data
public class FilterEntity {
//    @Id
    private UUID id;

    private String name;

    private String description;

    private String status;
//    @CreatedDate
//    private LocalDateTime created_at;
//
//    @LastModifiedDate
//    private LocalDateTime updated_at;
}
