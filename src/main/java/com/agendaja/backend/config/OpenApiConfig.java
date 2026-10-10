
package com.agendaja.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
		info = @Info(
				title = "AgendaJá API",
				version = "1.0.0",
				description = "API REST da plataforma AgendaJá para gestão de estabelecimentos e agendamentos."
		)
)
public class OpenApiConfig {
}
