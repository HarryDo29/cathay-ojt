package com.cathay.apigateway.entity;

import lombok.Data;
import java.util.UUID;

@Data
public class ServiceEntity {
//    @Id
    private UUID id;

    private String name;

//    @Column("base_path")
    private String path; // save as /api/v1/{service}/** service is name field

//    @Column("base_url")
    private String url; // save as http://host:port

    private Integer strip_prefix;

//    @Column("is_enabled")
    private boolean enabled;

//    @CreatedDate
//    private LocalDateTime created_at;
//
//    @LastModifiedDate
//    private LocalDateTime updated_at;
}
