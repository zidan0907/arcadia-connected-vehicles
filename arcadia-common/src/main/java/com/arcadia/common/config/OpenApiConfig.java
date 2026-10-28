package com.arcadia.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI arcadiaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Arcadia Connected Vehicle Platform API")
                .description("REST APIs for vehicle remote operations, eco-driving, predictive maintenance, and authentication.")
                .version("v1")
                .contact(new Contact().name("Arcadia Platform Team")));
    }
}
