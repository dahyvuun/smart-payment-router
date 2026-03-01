package com.dahyvuun.payment_router.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 / Swagger UI configuration.
 *
 * Access Swagger UI at: http://localhost:8080/swagger-ui.html
 * Access API docs at:   http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(buildInfo())
            .servers(buildServers())
            .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
            .components(new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, buildSecurityScheme()));
    }

    private Info buildInfo() {
        return new Info()
            .title("Payment Router API")
            .version("1.0.0")
            .description("""
                    ## Payment Router Service
                    
                    A fintech backend service that intelligently routes payments through
                    multiple payment gateways based on configurable strategies.
                    
                    ### Key Features
                    - **JWT Authentication** - Secure token-based auth
                    - **Multi-currency Wallets** - Support for USD, EUR, GBP, KRW and more
                    - **Smart Payment Routing** - LOWEST_FEE and HIGHEST_SUCCESS_RATE strategies
                    - **Idempotency** - Safe retry with X-Idempotency-Key header
                    - **Circuit Breaker** - Resilience4j fault tolerance
                    - **Rate Limiting** - Bucket4j token bucket algorithm
                    
                    ### Authentication
                    1. Register via `POST /api/auth/register`
                    2. Login via `POST /api/auth/login` to get a JWT token
                    3. Click **Authorize** and enter: `Bearer <your-token>`
                    """)
            .contact(new Contact()
                .name("Payment Router")
                .url("https://github.com/dahyvuun/payment-router"))
            .license(new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT"));
    }

    private List<Server> buildServers() {
        return List.of(
            new Server()
                .url("http://localhost:8080")
                .description("Local development server")
        );
    }

    private SecurityScheme buildSecurityScheme() {
        return new SecurityScheme()
            .name(SECURITY_SCHEME_NAME)
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("Enter your JWT token. Example: Bearer eyJhbGci...");
    }
}