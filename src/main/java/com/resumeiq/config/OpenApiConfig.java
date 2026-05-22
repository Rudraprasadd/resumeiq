package com.resumeiq.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Paste your JWT access token here (from /api/auth/login)"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("ResumeIQ API")
                .description("AI-powered resume analysis platform. " +
                             "Register → Login → Upload Resume → Run Analysis.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("ResumeIQ")
                    .email("support@resumeiq.in")));
    }
}