package com.cathay.apigateway.entity;

import lombok.Data;
import java.util.UUID;

//@Table("allowed_headers")
@Data
public class HeaderRulesEntity {
//    @Id
    private UUID id;

    private String name;

    private Integer max_length;

    private String pattern;

    private String description;

//    @CreatedDate
//    private LocalDateTime created_at;
//
//    @LastModifiedDate
//    private LocalDateTime updated_at;
}
