package com.zigu.ziguwas.swagger;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "ZIGU API", description = "ZIGU API 명세서", version = "v1"),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        paramName = "Authorization"
)
public class SwaggerConfig {

    @Value("${app.swagger.server-url}")
    private String swaggerServerUrl;

    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI()
                .servers(List.of(new Server().url(swaggerServerUrl)));
    }

    @Bean
    public GroupedOpenApi openApi() {
        return GroupedOpenApi.builder()
                .group("ZIGU API v1")
                .pathsToMatch("/api/**")
                .build();
    }
}
