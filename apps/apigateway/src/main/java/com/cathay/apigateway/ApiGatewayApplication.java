package com.cathay.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
//import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
//import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication
//@EnableR2dbcAuditing
//@EnableR2dbcRepositories(basePackages = "com.cathay.apigateway.repository")
@ConfigurationPropertiesScan
public class ApiGatewayApplication {
	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}
}
