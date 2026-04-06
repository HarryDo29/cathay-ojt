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
                .option(ConnectionFactoryOptions.DRIVER, "postgres")
                .option(ConnectionFactoryOptions.HOST, host)
                .option(ConnectionFactoryOptions.PORT, Integer.parseInt(port))
                .option(ConnectionFactoryOptions.USER, username)
                .option(ConnectionFactoryOptions.PASSWORD, password)
                .option(ConnectionFactoryOptions.DATABASE, "postgres")
                .build();

        ConnectionFactory factory = ConnectionFactories.get(options);

        ConnectionPoolConfiguration poolConfig = ConnectionPoolConfiguration.builder(factory)
                .initialSize(0)
                .maxSize(2)
                .maxIdleTime(Duration.ofMinutes(1))
                .validationQuery("SELECT 1")
                .build();

        return new ConnectionPool(poolConfig);
    }
}
