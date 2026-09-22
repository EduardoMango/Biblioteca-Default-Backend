package com.EduardoMango.Biblioteca.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Sistema de Gestión de Biblioteca - API REST",
                version = "v1.0.0",
                description = "API RESTful integral para la administración del catálogo bibliográfico, control de inventario físico y stock disponible, " +
                              "gestión transaccional del ciclo de vida de préstamos y devoluciones, sincronización reactiva con Google Books API " +
                              "y seguridad mediante tokens JWT con control de acceso basado en roles (RBAC).",
                contact = @Contact(
                        name = "Eduardo Mango",
                        email = "eduardomango08@gmail.com"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        )
)
@SecurityScheme(
        name = "bearerAuth",
        description = "Autenticación basada en JSON Web Token (JWT). Ingrese el token de acceso obtenido en el endpoint de autenticación (/api/v1/auth/login) con el prefijo Bearer.",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}

