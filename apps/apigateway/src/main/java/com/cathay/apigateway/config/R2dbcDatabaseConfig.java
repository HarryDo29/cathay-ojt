package com.cathay.apigateway.config;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;

import java.sql.Driver;
import java.time.Duration;

@Configuration
public class R2dbcDatabaseConfig extends AbstractR2dbcConfiguration {
    @Value("${jdbc.driver}")
    private String driver;

    @Value("${jdbc.database}")
    private String database;

    @Value("${jdbc.host}")
    private String host;

    @Value("${jdbc.port}")
    private String port;

    @Value("${jdbc.username}")
    private String username;

    @Value("${jdbc.password}")
    private String password;

    @Bean
    @Override
    public ConnectionFactory connectionFactory() {
        ConnectionFactoryOptions options = ConnectionFactoryOptions
                .builder()
                .option(ConnectionFactoryOptions.DRIVER, driver)
                .option(ConnectionFactoryOptions.HOST, host)
                .option(ConnectionFactoryOptions.PORT, Integer.parseInt(port))
                .option(ConnectionFactoryOptions.USER, username)
                .option(ConnectionFactoryOptions.PASSWORD, password)
                .option(ConnectionFactoryOptions.DATABASE, database)
                .build();

        ConnectionFactory factory = ConnectionFactories.get(options);

        ConnectionPoolConfiguration poolConfig = ConnectionPoolConfiguration.builder(factory)
                .initialSize(1) // lúc khởi động chỉ cần 1 connection
                .maxSize(2) // tối đa 2 connection để tiết kiệm tài nguyên, vì API Gateway thường không cần nhiều kết nối đến DB
                .maxIdleTime(Duration.ofMinutes(1)) // thời gian đóng connection nếu rảnh trong vongf 1 phút
                .validationQuery("SELECT 1") // câu lệnh đơn giản để kiểm tra kết nối còn sống hay không
                .build();

        return new ConnectionPool(poolConfig);
    }
}
