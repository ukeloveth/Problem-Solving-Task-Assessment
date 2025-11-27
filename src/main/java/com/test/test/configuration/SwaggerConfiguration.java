package com.test.test.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI configuration for API documentation
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Task Management API",
                version = "1.0",
                description = "RESTful API for managing tasks with hierarchical structure. " +
                        "Supports CRUD operations, task hierarchy (up to 5 levels), " +
                        "and comprehensive validation."
        ),
        servers = { 
            @Server(url = "/", description = "Default Server URL")
        }
)
public class SwaggerConfiguration {
}