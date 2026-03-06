package com.cathay.apigateway.data.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@Data
@ConfigurationProperties(prefix = "app.limits")
public class LimitPropertiesConfig {
    private DataSize max_body_size = DataSize.ofMegabytes(10);
    private DataSize max_header_sizes = DataSize.ofKilobytes(8);
    private int max_query_params = 50;
}
