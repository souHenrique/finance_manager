package com.amorim.finance_manager.config.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    OpenAPI financeManagerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Finance Manager API")
                        .description("""
                                API REST para gerenciamento financeiro pessoal.
                                
                                Permite gerenciar usuários, contas, categorias,
                                receitas, despesas, PIX e transferências.
                                """)
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ));
    }
}
