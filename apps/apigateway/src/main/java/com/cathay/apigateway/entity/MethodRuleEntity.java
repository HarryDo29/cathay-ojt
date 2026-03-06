package com.cathay.apigateway.entity;

import lombok.Data;
import java.util.UUID;

//@Table("allowed_headers")
@Data
public class MethodRuleEntity {
//    @Id
    private UUID id;

    private String method;

    private boolean require_body;

    private boolean require_content_type;

    private long max_body_size;

//    @CreatedDate
//    private LocalDateTime created_at;
//
//    @LastModifiedDate
//    private LocalDateTime updated_at;
}
